package core

import (
	"fmt"
	"log"
	"net/http"
	"os"
	"time"

	"github.com/anacrolix/missinggo/refclose"
	"github.com/anacrolix/torrent"
	"github.com/anacrolix/torrent/metainfo"
)

// Handles ref counting, close grace, and various torrent client wrapping
// work.
func getTorrentHandle(r *http.Request, ih metainfo.Hash) *torrent.Torrent {
	var ref *refclose.Ref
	grace := torrentCloseGraceForRequest(r)
	if grace >= 0 {
		ref = torrentRefs.NewRef(ih)
	}
	tc := torrentClientForRequest(r)
	t, new := tc.AddTorrentInfoHash(ih)
	if grace >= 0 {
		ref.SetCloser(t.Drop)
		go func() {
			defer time.AfterFunc(grace, ref.Release)
			<-r.Context().Done()
		}()
	}
	if new {
		mi := cachedMetaInfo(ih)
		if mi != nil {
			t.AddTrackers(mi.UpvertedAnnounceList())
			t.SetInfoBytes(mi.InfoBytes)
		}
	}
	return t
}

func cachedMetaInfo(infoHash metainfo.Hash) *metainfo.MetaInfo {
	p := fmt.Sprintf("torrents/%s.torrent", infoHash.HexString())
	mi, err := metainfo.LoadFromFile(p)
	if os.IsNotExist(err) {
		return nil
	}
	if err != nil {
		log.Printf("error loading metainfo file %q: %s", p, err)
	}
	return mi
}
