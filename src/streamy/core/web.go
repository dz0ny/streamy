package core

import (
	"fmt"
	"strings"

	"github.com/anacrolix/torrent"
	"github.com/anacrolix/torrent/metainfo"
)

type WebURLs struct {
	HTML   string `json:"html"`
	Play   string `json:"play"`
	Events string `json:"ws_events"`
}

func NewWebURLs(hex string) WebURLs {
	return WebURLs{
		Play:   urlFor("play", hex),
		HTML:   urlFor("html", hex),
		Events: urlFor("events", hex),
	}
}

type fileInfo struct {
	metainfo.FileInfo
	InfoUrl   string `json:"info"`
	StreamUrl string `json:"data"`
}

type TorrentWeb struct {
	Name     string `json:"name"`
	InfoHash string `json:"ih"`

	Files []fileInfo           `json:"files"`
	Stats torrent.TorrentStats `json:"stats"`
	URLs  WebURLs              `json:"urls"`
}

func NewTorrentWeb(t *torrent.Torrent) (tr TorrentWeb) {
	hex := t.InfoHash().HexString()
	var files []fileInfo

	for _, f := range t.Info().Files {
		path_ := strings.Join(f.Path, "/")
		pi := fmt.Sprintf("?path=%s", path_)
		fi := fileInfo{f, urlFor("file", hex) + pi, urlFor("stream", hex) + pi}
		files = append(files, fi)
	}
	tr = TorrentWeb{
		Name:     t.String(),
		InfoHash: hex,
		Files:    files,
		Stats:    t.Stats(),
		URLs:     NewWebURLs(hex),
	}
	return
}

func urlFor(name, ih string) string {
	ret, _ := router.Get(name).URL("ih", ih)
	return ret.String()
}
