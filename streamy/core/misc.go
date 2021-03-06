package core

import (
	"fmt"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"sort"
	"time"

	"github.com/anacrolix/missinggo"
	"github.com/anacrolix/torrent"
)

// Path is the given request path.
func torrentFileByPath(t *torrent.Torrent, path_ string) *torrent.File {
	for _, f := range t.Files() {
		if f.DisplayPath() == path_ {
			return f
		}
	}
	return nil
}

func saveTorrentFile(t *torrent.Torrent) (err error) {
	p := filepath.Join(StorageRoot, "torrents", t.InfoHash().HexString()+".torrent")
	ps := p + ".save"
	os.MkdirAll(filepath.Dir(ps), 0750)
	f, err := os.OpenFile(ps, os.O_CREATE|os.O_WRONLY|os.O_TRUNC, 0660)
	if err != nil {
		return err
	}
	err = t.Metainfo().Write(f)
	if err != nil {
		f.Close()
		return
	}
	err = f.Close()
	if err != nil {
		return
	}
	return os.Rename(ps, p)
}

func listTorrents(c *torrent.Client) ([]TorrentWeb, error) {
	var torrents []TorrentWeb
	if torrentsC := c.Torrents(); torrentsC != nil {
		for _, t := range torrentsC {
			torrents = append(torrents, NewTorrentWeb(t))
		}
		sort.SliceStable(torrents, func(i, j int) bool { return torrents[i].InfoHash < torrents[j].InfoHash })
	}
	return torrents, nil
}

func getTorrentClientFromRequestContext(r *http.Request) *torrent.Client {
	return r.Context().Value(torrentClientContextKey).(*torrent.Client)
}

func serveTorrent(w http.ResponseWriter, r *http.Request, t *torrent.Torrent) {
	select {
	case <-t.GotInfo():
	case <-r.Context().Done():
		return
	}
	w.Header().Set("Content-Disposition", fmt.Sprintf(`attachment; filename="%s"`, t.Name()))
	serveReader(w, r, t.NewReader(), t.Name())
}

func serveReader(w http.ResponseWriter, r *http.Request, tr torrent.Reader, name string) {
	defer tr.Close()
	tr.SetReadahead(48 << 20)
	rs := struct {
		io.Reader
		io.Seeker
	}{
		Reader: missinggo.ContextedReader{
			R:   tr,
			Ctx: r.Context(),
		},
		Seeker: tr,
	}
	http.ServeContent(w, r, name, time.Time{}, rs)
}

func serveFile(w http.ResponseWriter, r *http.Request, t *torrent.Torrent, _path string) {
	tf := torrentFileByPath(t, _path)
	if tf == nil {
		http.Error(w, "file not found", http.StatusNotFound)
		return
	}
	w.Header().Set("Content-Disposition", fmt.Sprintf(`attachment; filename="%s"`, tf.DisplayPath()))
	serveReader(w, r, tf.NewReader(), _path)
}
