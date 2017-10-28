package core

import (
	"fmt"
	"io"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"time"

	"github.com/anacrolix/missinggo"
	"github.com/anacrolix/missinggo/httptoo"
	"github.com/anacrolix/torrent"
	"github.com/anacrolix/torrent/metainfo"
)

// Path is the given request path.
func torrentFileByPath(t *torrent.Torrent, path_ string) *torrent.File {
	for _, f := range t.Files() {
		if f.DisplayPath() == path_ {
			return &f
		}
	}
	return nil
}

func saveTorrentFile(t *torrent.Torrent) (err error) {
	p := filepath.Join("torrents", t.InfoHash().HexString()+".torrent")
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

func listTorrents() ([]TorrentWeb, error) {
	var torrents []TorrentWeb
	walkFn := func(path string, info os.FileInfo, err error) error {
		if err != nil {
			return err
		}
		if !info.IsDir() && strings.HasSuffix(info.Name(), ".torrent") {
			mi, err := metainfo.LoadFromFile(path)
			if err != nil {
				return err
			}
			t, err := mi.UnmarshalInfo()
			hex := mi.HashInfoBytes().HexString()
			var files []fileInfo
			for _, f := range t.Files {
				path_ := strings.Join(f.Path, "/")
				pi := fmt.Sprintf("?path=%s", path_)
				fi := fileInfo{f, urlFor("file", hex) + pi, urlFor("stream", hex) + pi}
				files = append(files, fi)
			}
			tr := TorrentWeb{
				Name:     t.Name,
				InfoHash: hex,
				Files:    files,
				URLs:     NewWebURLs(hex),
			}
			torrents = append(torrents, tr)
		}
		return nil
	}
	if _, err := os.Stat("torrents"); os.IsNotExist(err) {
		return torrents, nil
	}
	err := filepath.Walk("torrents", walkFn)
	if err != nil {
		return torrents, err
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
	serveTorrentSection(w, r, t, 0, t.Length(), t.Name())
}

func serveTorrentSection(w http.ResponseWriter, r *http.Request, t *torrent.Torrent, offset, length int64, name string) {
	tr := t.NewReader()
	defer tr.Close()
	tr.SetReadahead(48 << 20)
	rs := missinggo.NewSectionReadSeeker(struct {
		io.Reader
		io.Seeker
	}{
		Reader: missinggo.ContextedReader{
			R:   tr,
			Ctx: r.Context(),
		},
		Seeker: tr,
	}, offset, length)
	http.ServeContent(w, r, name, time.Time{}, rs)
}

func serveFile(w http.ResponseWriter, r *http.Request, t *torrent.Torrent, _path string) {
	tf := torrentFileByPath(t, _path)
	if tf == nil {
		http.Error(w, "file not found", http.StatusNotFound)
		return
	}
	w.Header().Set("ETag", httptoo.EncodeQuotedString(fmt.Sprintf("%s/%s", t.InfoHash().HexString(), _path)))
	serveTorrentSection(w, r, t, tf.Offset(), tf.Length(), _path)
}
