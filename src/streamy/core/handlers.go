package core

import (
	"fmt"
	"log"
	"net/http"
	"net/url"
	"strings"

	"github.com/go-chi/chi"
	"github.com/go-chi/render"
	"github.com/rakyll/statik/fs"

	_ "streamy/statik" // UI

	"github.com/anacrolix/torrent"
	"github.com/anacrolix/torrent/metainfo"
	"golang.org/x/net/websocket"
)

// displayName: Get file
// description: Serve file from torrent
// uriParameters:
//   path:
//     description: path to file in torrent
//     type: string
//     required: true
//     example: screenshots/screenshot1.jpg
func dataHandler(w http.ResponseWriter, r *http.Request) {
	q := r.URL.EscapedPath()
	q = strings.Split(q, "/stream/")[1]

	t := torrentForRequest(r)
	select {
	case <-t.GotInfo():
	case <-r.Context().Done():
		return
	}

	if len(q) == 0 {
		serveTorrent(w, r, t)
	} else {
		serveFile(w, r, t, q)
	}
}

// displayName: List torrents
// description: Return list of torrents
func statusHandler(w http.ResponseWriter, r *http.Request) {
	c := getTorrentClientFromRequestContext(r)
	torrents, err := listTorrents(c)
	if err != nil {
		http.Error(w, fmt.Sprintf("%s", err), http.StatusBadRequest)
		return
	}
	render.JSON(w, r, torrents)
}

// displayName: Add magnet
// description: Add torrent via magnet link
// uriParameters:
//   magnet:
//     description: urlencoded magnet link
//     type: string
//     required: true
//     example: magnet%3A%3Fxt%3Durn%3Abtih%3A40448d478d9203a3919b0900e7fbb9e8748dcdf9%26dn%3Dubuntu.iso
func addMagnetHandler(w http.ResponseWriter, r *http.Request) {
	c := getTorrentClientFromRequestContext(r)
	if uri := r.URL.Query().Get("magnet"); uri != "" {
		uri, _ = url.QueryUnescape(uri)
		if t, err := c.AddMagnet(uri); err == nil {
			select {
			case <-t.GotInfo():
				if err := saveTorrentFile(t); err != nil {
					log.Printf("error saving torrent file: %s", err)
				}
				break
			case <-r.Context().Done():
				break
			}
			trackers := [][]string{
				[]string{"udp://tracker.cyberia.is:6969/announce"},
				[]string{"udp://tracker.coppersurfer.tk:6969/announce"},
				[]string{"udp://tracker.open-internet.nl:6969/announce"},
				[]string{"udp://p4p.arenabg.com:1337/announce"},
				[]string{"udp://tracker.internetwarriors.net:1337/announce"},
				[]string{"udp://tracker.skyts.net:6969/announce"},
				[]string{"udp://tracker.safe.moe:6969/announce"},
				[]string{"udp://tracker.piratepublic.com:1337/announce"},
				[]string{"udp://tracker.opentrackr.org:1337/announce"},
				[]string{"udp://allesanddro.de:1337/announce"},
				[]string{"udp://9.rarbg.to:2710/announce"},
				[]string{"udp://tracker2.christianbro.pw:6969/announce"},
				[]string{"udp://tracker1.wasabii.com.tw:6969/announce"},
				[]string{"udp://tracker.zer0day.to:1337/announce"},
				[]string{"udp://public.popcorn-tracker.org:6969/announce"},
				[]string{"udp://inferno.demonoid.pw:3418/announce"},
				[]string{"udp://tracker.xku.tv:6969/announce"},
				[]string{"udp://tracker.vanitycore.co:6969/announce"},
				[]string{"udp://tracker.mg64.net:6969/announce"},
				[]string{"udp://open.facedatabg.net:6969/announc"},
			}
			t.AddTrackers(trackers)
			http.Redirect(w, r, fmt.Sprintf("/torrents/%s", t.InfoHash().HexString()), 301)
		} else {
			http.Error(w, err.Error(), http.StatusBadRequest)
			return
		}
	} else {
		http.Error(w, "missing magnet param", http.StatusBadRequest)
		return
	}
}

// displayName: Add torrent
// description: Add torrent via file upload
// body:
//   torrent:
//     description: torrent content
//     type: file
func addTorrentHandler(w http.ResponseWriter, r *http.Request) {
	c := getTorrentClientFromRequestContext(r)

	r.ParseMultipartForm(10 << 6)
	if headers, ok := r.MultipartForm.File["torrent"]; ok {
		if fh, err := headers[0].Open(); err == nil {
			mi, err := metainfo.Load(fh)
			if err != nil {
				http.Error(w, err.Error(), http.StatusBadRequest)
				return
			}

			if t, err := c.AddTorrent(mi); err == nil {
				select {
				case <-t.GotInfo():
					err := saveTorrentFile(t)
					if err != nil {
						log.Printf("error saving torrent file: %s", err)
					}
					break
				case <-r.Context().Done():
					break
				}
				http.Redirect(w, r, fmt.Sprintf("/torrents/%s", t.InfoHash().HexString()), 301)
			}
		} else {
			http.Error(w, "missing torrent payload", http.StatusBadRequest)
			return
		}
	}
}

// displayName: Torrent info
// description: Return info about torrent
func infoTorrent(w http.ResponseWriter, r *http.Request) {
	t := torrentForRequest(r)
	select {
	case <-t.GotInfo():
	case <-r.Context().Done():
		return
	}

	render.JSON(w, r, NewTorrentWeb(t))
}

// displayName: Torrent prefetch
// description: Prefetch all torrent data
func startTorrent(w http.ResponseWriter, r *http.Request) {
	t := torrentForRequest(r)
	select {
	case <-t.GotInfo():
		break
	}

	t.DownloadAll()
	render.JSON(w, r, NewTorrentWeb(t))
}

// displayName: Torrent stop
// description: Stop torrent download
func stopTorrent(w http.ResponseWriter, r *http.Request) {
	t := torrentForRequest(r)
	select {
	case <-t.GotInfo():
		break
	}
	t.CancelPieces(0, t.NumPieces())
	render.JSON(w, r, NewTorrentWeb(t))
}

// displayName: Torrent drop
// description: Delete torrent
func deleteTorrent(w http.ResponseWriter, r *http.Request) {
	t := torrentForRequest(r)
	select {
	case <-t.GotInfo():
	case <-r.Context().Done():
		return
	}
	t.Drop()
	render.PlainText(w, r, "OK")
}

// displayName: Torrent status
// description: Subscribe to torrent updates
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

// displayName: Get file info
// description: Torrent info about file
// uriParameters:
//   path:
//     description: path to file in torrent
//     type: string
//     required: true
//     example: screenshots/screenshot1.jpg
func fileStateHandler(w http.ResponseWriter, r *http.Request) {
	tPath := r.URL.EscapedPath()
	tPath = strings.Split(tPath, "/stream/")[1]

	f := torrentFileByPath(torrentForRequest(r), tPath)
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

func fileServerHandler(r chi.Router) {
	statikFS, err := fs.New()
	if err != nil {
		log.Fatal(err)
	}

	staticHandler := http.FileServer(statikFS)
	path := "/static"

	if path != "/" && path[len(path)-1] != '/' {
		r.Get(path, http.RedirectHandler(path+"/", 301).ServeHTTP)
		path += "/"
	}
	path += "*"
	r.Get(path, http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		staticHandler.ServeHTTP(w, r)
	}))
	r.Get("/", func(w http.ResponseWriter, r *http.Request) {
		r.URL.Path = "/"
		staticHandler.ServeHTTP(w, r)
	})
}
