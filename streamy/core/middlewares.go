package core

import (
	"net/http"
	"time"

	"github.com/anacrolix/missinggo/refclose"
	"github.com/anacrolix/torrent"
	"github.com/anacrolix/torrent/metainfo"
)

// Handles ref counting, close grace, and various torrent client wrapping
// work.
func getTorrentHandle(r *http.Request, ih metainfo.Hash) *torrent.Torrent {
	tc := torrentClientForRequest(r)
	t, _ := tc.AddTorrentInfoHash(ih)
	return t
}

// Handles ref counting, close grace, and various torrent client wrapping
// work.
func getTorrentHandleCloser(r *http.Request, ih metainfo.Hash) *torrent.Torrent {
	var ref *refclose.Ref
	grace := torrentCloseGraceForRequest(r)
	if grace >= 0 {
		ref = torrentRefs.NewRef(ih)
	}
	tc := torrentClientForRequest(r)
	t, _ := tc.AddTorrentInfoHash(ih)
	if grace >= 0 {
		ref.SetCloser(t.Drop)
		go func() {
			defer time.AfterFunc(grace, ref.Release)
			<-r.Context().Done()
		}()
	}

	return t
}
