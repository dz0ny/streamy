package core

import (
	"fmt"
	"strings"

	"github.com/anacrolix/torrent"
	"github.com/anacrolix/torrent/metainfo"
)

type WebURLs struct {
	Events string `json:"ws_events"`
}

func NewWebURLs(hex string) WebURLs {
	return WebURLs{
		Events: urlFor("events", hex),
	}
}

type fileInfo struct {
	metainfo.FileInfo
	InfoURL   string `json:"info"`
	StreamURL string `json:"data"`
}

type TorrentWeb struct {
	Name     string `json:"name"`
	InfoHash string `json:"ih"`

	Files []fileInfo           `json:"files"`
	Stats torrent.TorrentStats `json:"stats"`
	URLs  WebURLs              `json:"urls"`

	Downloaded int64
	Missing    int64
	Seeding    bool
}

func NewTorrentWeb(t *torrent.Torrent) (tr TorrentWeb) {
	hex := t.InfoHash().HexString()
	var files []fileInfo

	for _, f := range t.Info().Files {
		path := "/" + strings.Join(f.Path, "/")

		fi := fileInfo{
			f,
			urlFor("file", hex) + path,
			urlFor("stream", hex) + path,
		}
		files = append(files, fi)
	}
	tr = TorrentWeb{
		Name:       t.String(),
		InfoHash:   hex,
		Files:      files,
		Stats:      t.Stats(),
		URLs:       NewWebURLs(hex),
		Downloaded: t.BytesCompleted(),
		Missing:    t.BytesMissing(),
		Seeding:    t.Seeding(),
	}
	return
}

func urlFor(name, ih string) string {
	return fmt.Sprintf("/torrents/%s/%s", ih, name)
}
