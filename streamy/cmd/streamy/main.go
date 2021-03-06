package main

import (
	"fmt"
	"log"
	"net"
	"net/http"
	"time"

	"streamy/core"
	"streamy/version"

	"github.com/anacrolix/missinggo/v2/filecache"
	"github.com/anacrolix/missinggo/v2/resource"
	"github.com/anacrolix/missinggo/x"
	"github.com/anacrolix/tagflag"
	"github.com/anacrolix/torrent"
	"github.com/anacrolix/torrent/storage"
	"github.com/jackpal/gateway"
	natpmp "github.com/jackpal/go-nat-pmp"
)

const oneMonth = 262974328

var flags = struct {
	Addr                  string        `help:"HTTP listen address"`
	CacheCapacity         tagflag.Bytes `help:"Data cache capacity"`
	TorrentGrace          time.Duration `help:"How long to wait to drop a torrent after its last request"`
	FileDir               string        `help:"Piece storage path"`
	Seed                  bool          `help:"Seed data"`
	UploadRate            tagflag.Bytes `help:"Upload rate limit"`
	DownloadRate          tagflag.Bytes `help:"Download rate limit"`
	Prefetch              tagflag.Bytes `help:"Prefetch limit"`
	Debug                 bool          `help:"Verbose output"`
	ConnectionsPerTorrent int           `help:"Limit Connections per torrent"`
	MountDir              string        `help:"location where torrent contents are made available"`
	DisableUPNP           bool          `help:"Should we use UPNP to open ports"`
}{
	Addr:                  "0.0.0.0:9092",
	CacheCapacity:         4000 << 20,
	TorrentGrace:          time.Minute * 1,
	FileDir:               "filecache",
	UploadRate:            25600,
	DownloadRate:          1048576,
	ConnectionsPerTorrent: 40,
	Prefetch:              100 << 20,
	MountDir:              "",
	DisableUPNP:           false,
}

func getStorageProvider() resource.Provider {
	fc, err := filecache.NewCache(flags.FileDir)
	x.Pie(err)
	fc.SetCapacity(flags.CacheCapacity.Int64())
	return fc.AsResourceProvider()
}

func getStorage() (_ storage.ClientImpl, onTorrentGrace func(torrent.InfoHash)) {
	return storage.NewResourcePieces(getStorageProvider()), func(ih torrent.InfoHash) {}
}

func newTorrentClient(freePort int) (ret *torrent.Client, err error) {
	core.StorageRoot = flags.FileDir

	conf := torrent.NewDefaultClientConfig()
	storage, _ := getStorage()
	conf.DefaultStorage = storage
	conf.DisableTCP = false
	conf.DisableIPv6 = false
	conf.DisableUTP = true
	conf.DisableAcceptRateLimiting = true
	conf.ListenPort = freePort
	conf.EstablishedConnsPerTorrent = flags.ConnectionsPerTorrent

	conf.ExtendedHandshakeClientVersion = "Transmission/2.92"
	conf.HTTPUserAgent = "Transmission/2.92"
	conf.Bep20 = "-TR2920-"
	conf.NoDefaultPortForwarding = flags.DisableUPNP
	return torrent.NewClient(conf)
}

func getPort() int {
	addr, err := net.ResolveTCPAddr("tcp", "localhost:0")
	if err != nil {
		panic(err)
	}

	l, err := net.ListenTCP("tcp", addr)
	if err != nil {
		panic(err)
	}
	defer l.Close()
	return l.Addr().(*net.TCPAddr).Port
}

func main() {
	log.SetFlags(log.Flags() | log.Lshortfile)
	desc := tagflag.Description(fmt.Sprintf(
		"Streamy %s built at %s from commit %s@%s",
		version.Version, version.BuildTime, version.CommitHash, version.Branch,
	))
	tagflag.Parse(&flags, desc)

	freePort := getPort()

	log.Printf("Torrent client port: %d", freePort)
	gatewayIP, err := gateway.DiscoverGateway()
	if err != nil {
		return
	}

	client := natpmp.NewClient(gatewayIP)

	if _, err = client.AddPortMapping("tcp", freePort, freePort, oneMonth); err != nil {
		log.Fatalf("error creating natpmp mapping: %s", err)
	}

	if _, err = client.AddPortMapping("udp", freePort, freePort, oneMonth); err != nil {
		log.Fatalf("error creating natpmp mapping: %s", err)
	}
	cl, err := newTorrentClient(freePort)
	if err != nil {
		log.Fatalf("error creating torrent client: %s", err)
	}
	defer cl.Close()
	defer func() {
		client.AddPortMapping("tcp", freePort, freePort, 0)
		client.AddPortMapping("udp", freePort, freePort, 0)
	}()
	l, err := net.Listen("tcp4", flags.Addr)
	if err != nil {
		log.Fatal(err)
	}
	defer l.Close()

	log.Printf("serving http at %s", l.Addr())

	h := &core.Handler{
		TC:                cl,
		TorrentCloseGrace: flags.TorrentGrace,
	}
	err = http.Serve(l, h)
	if err != nil {
		log.Fatal(err)
	}
}
