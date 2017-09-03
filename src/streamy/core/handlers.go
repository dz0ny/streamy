package core

import (
	"fmt"
	"html/template"
	"log"
	"net/http"
	"strings"

	json "github.com/json-iterator/go"

	"github.com/anacrolix/torrent"
	"github.com/anacrolix/torrent/metainfo"
	"golang.org/x/net/websocket"
)

var html = `
<!DOCTYPE html>
<html lang="en">
  <head>
    <!-- Required meta tags -->
    <meta charset="utf-8">
    <meta name="viewport" content="width=device-width, initial-scale=1, shrink-to-fit=no">
	<style>
	#playerWrapper {
		background-color: #000;
		font-size: 18px;
	}
	body {
		padding-top: 1rem;
		background-color: #000!important;
	  }
	#streamy-player {
		text-align: center;
	}
	</style>
    <!-- Bootstrap CSS -->
    <link rel="stylesheet" href="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0-beta/css/bootstrap.min.css" integrity="sha384-/Y6pD6FV/Vv2HJnA6t+vslU6fwYXjCFtcEpHbNJ0lyAFsXTsjBbfaDjzALeQsN6M" crossorigin="anonymous">
  </head>
  <body>

<div class="container">
	<h1>{{.Name}}</h1>
	<div id="playerWrapper">
		<div id="streamy-player">
			<video preload="auto" controls src="{{.URLs.Play}}"></video>
		</div>
	</div>

</div><!-- /.container -->

    <!-- Optional JavaScript -->
    <!-- jQuery first, then Popper.js, then Bootstrap JS -->
    <script src="https://code.jquery.com/jquery-3.2.1.slim.min.js" integrity="sha384-KJ3o2DKtIkvYIK3UENzmM7KCkRr/rE9/Qpg6aAZGJwFDMVNA/GpGFF93hXpG5KkN" crossorigin="anonymous"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/popper.js/1.11.0/umd/popper.min.js" integrity="sha384-b/U6ypiBEHpOf/4+1nzFpr53nxSS+GLCkfwBdFNTxtclqqenISfwAzpKaMNFNmj4" crossorigin="anonymous"></script>
    <script src="https://maxcdn.bootstrapcdn.com/bootstrap/4.0.0-beta/js/bootstrap.min.js" integrity="sha384-h0AbiXch4ZDo7tp9hKZ4TsHbi047NrKGLO3SEJAg45jXxnGIfYzk4Si90RDIqNm1" crossorigin="anonymous"></script>
	<script src="https://cdn.plyr.io/2.0.13/plyr.js"></script>
	<link rel="stylesheet" href="https://cdn.plyr.io/2.0.13/plyr.css">
	<script>
		plyr.setup();
		document.getElementById("streamy-player").height = document.documentElement.clientHeight;
	</script>
	</body>
</html>`

func htmlHandler(w http.ResponseWriter, r *http.Request) {
	t := torrentForRequest(r)
	s := template.New("video")
	s, _ = s.Parse(html)
	fmt.Println(s.ExecuteTemplate(w, "video", NewTorrentWeb(t)))
}

func dataHandler(w http.ResponseWriter, r *http.Request) {
	q := r.URL.Query()
	t := torrentForRequest(r)
	if len(q["path"]) == 0 {
		serveTorrent(w, r, t)
	} else {
		serveFile(w, r, t, q.Get("path"))
	}
}

func playHandler(w http.ResponseWriter, r *http.Request) {
	t := torrentForRequest(r)
	select {
	case <-t.GotInfo():
	case <-r.Context().Done():
		return
	}
	toplay := ""
	for _, f := range t.Info().Files {
		name := strings.Join(f.Path, "/")
		if strings.HasSuffix(name, ".mp4") {
			toplay = name
			break
		}
		if strings.HasSuffix(name, ".mkv") {
			toplay = name
			break
		}
		if strings.HasSuffix(name, ".avi") {
			toplay = name
			break
		}
	}
	serveFile(w, r, t, toplay)
}

func pingHandler(w http.ResponseWriter, r *http.Request) {
	fmt.Fprintf(w, "pong %s\n", r.URL.Path)
}

func statusHandler(w http.ResponseWriter, r *http.Request) {
	torrents, err := listTorrents()
	if err != nil {
		http.Error(w, fmt.Sprintf("%s", err), http.StatusBadRequest)
		return
	}
	json.NewEncoder(w).Encode(torrents)
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

			http.Redirect(w, r, urlFor("info", t.InfoHash().HexString()), 301)
		}
	}
	// curl --form "torrent=@my-file.txt" http://localhost:8080/torrent/add
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
					http.Redirect(w, r, urlFor("info", t.InfoHash().HexString()), 301)
				}
			}
		}
	}
}

func infoHandler(w http.ResponseWriter, r *http.Request) {
	t := torrentForRequest(r)
	select {
	case <-t.GotInfo():
	case <-r.Context().Done():
		return
	}

	json.NewEncoder(w).Encode(NewTorrentWeb(t))
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
	json.NewEncoder(w).Encode(f.State())
}
