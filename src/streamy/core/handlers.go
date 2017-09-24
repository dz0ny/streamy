package core

import (
	"fmt"
	"log"
	"net/http"
	"strings"

	"github.com/go-chi/chi"
	"github.com/go-chi/render"

	"github.com/anacrolix/torrent"
	"github.com/anacrolix/torrent/metainfo"
	"golang.org/x/net/websocket"
)

func dataHandler(w http.ResponseWriter, r *http.Request) {
	q := r.URL.Query()
	t := torrentForRequest(r)
	if len(q["path"]) == 0 {
		serveTorrent(w, r, t)
	} else {
		serveFile(w, r, t, q.Get("path"))
	}
}

func statusHandler(w http.ResponseWriter, r *http.Request) {
	torrents, err := listTorrents()
	if err != nil {
		http.Error(w, fmt.Sprintf("%s", err), http.StatusBadRequest)
		return
	}
	render.JSON(w, r, torrents)
}

func addHandler(w http.ResponseWriter, r *http.Request) {
	c := getTorrentClientFromRequestContext(r)
	if uri := r.URL.Query().Get("magnet"); uri != "" {
		if t, err := c.AddMagnet(uri); err == nil {
			select {
			case <-t.GotInfo():
			case <-r.Context().Done():
				return
			}
			go saveTorrentWhenGotInfo(t)

			http.Redirect(w, r, fmt.Sprintf("/torrents/%s", t.InfoHash().HexString()), 301)
		}
	}
	// curl --form "torrent=@my-file.txt" http://localhost:8080/torrents/add
	if r.Method == "POST" {
		r.ParseMultipartForm(10 << 6)
		if headers, ok := r.MultipartForm.File["torrent"]; ok {
			if fh, err := headers[0].Open(); err == nil {
				mi, err := metainfo.Load(fh)
				if err != nil {
					return
				}

				if t, err := c.AddTorrent(mi); err == nil {
					select {
					case <-t.GotInfo():
					case <-r.Context().Done():
						return
					}
					go saveTorrentWhenGotInfo(t)
					http.Redirect(w, r, fmt.Sprintf("/torrents/%s", t.InfoHash().HexString()), 301)
				}
			}
		}
	}
}

func infoTorrent(w http.ResponseWriter, r *http.Request) {
	t := torrentForRequest(r)
	select {
	case <-t.GotInfo():
	case <-r.Context().Done():
		return
	}
	render.JSON(w, r, NewTorrentWeb(t))
}

func eventHandler(w http.ResponseWriter, r *http.Request) {
	t := torrentForRequest(r)
	select {
	case <-t.GotInfo():
	case <-r.Context().Done():
		return
	}
	s := t.SubscribePieceStateChanges()
	defer s.Close()
	websocket.Server{
		Handler: func(c *websocket.Conn) {
			defer c.Close()
			readClosed := make(chan struct{})
			go func() {
				defer close(readClosed)
				c.Read(nil)
			}()
			for {
				select {
				case <-readClosed:
					eventHandlerWebsocketReadClosed.Add(1)
					return
				case <-r.Context().Done():
					eventHandlerContextDone.Add(1)
					return
				case _i := <-s.Values:
					i := _i.(torrent.PieceStateChange).Index
					if err := websocket.JSON.Send(c, Event{PieceChanged: &i}); err != nil {
						log.Printf("error writing json to websocket: %s", err)
						return
					}
				}
			}
		},
	}.ServeHTTP(w, r)
}

func fileStateHandler(w http.ResponseWriter, r *http.Request) {
	path_ := r.URL.Query().Get("path")
	f := torrentFileByPath(torrentForRequest(r), path_)
	if f == nil {
		http.Error(w, "file not found", http.StatusNotFound)
		return
	}
	render.JSON(w, r, f.State())
}

func corsHandler(h http.Handler) http.Handler {
	return http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Access-Control-Allow-Origin", "*")
		w.Header().Set("Access-Control-Allow-Methods", "POST, GET, PUT, DELETE")
		w.Header().Set("Access-Control-Max-Age", "3600")
		w.Header().Set("Access-Control-Allow-Headers", "Content-Type, Access-Control-Allow-Headers, Authorization, X-Requested-With")
		w.Header().Set("Access-Control-Allow-Credentials", "true")

		if r.Method == "OPTIONS" {
			return
		}
		h.ServeHTTP(w, r)
	})
}

func fileServerHandler(r chi.Router, path string, root http.FileSystem) {
	if strings.ContainsAny(path, ":*") {
		panic("Server does not permit URL parameters.")
	}

	fs := http.StripPrefix(path, http.FileServer(root))

	if path != "/" && path[len(path)-1] != '/' {
		r.Get(path, http.RedirectHandler(path+"/", 301).ServeHTTP)
		path += "/"
	}
	path += "*"

	r.Get(path, http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		fs.ServeHTTP(w, r)
	}))
}
