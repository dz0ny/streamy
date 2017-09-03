package main

import (
	"fmt"
	"log"
	"net"
	"net/http"
	"time"

	"github.com/anacrolix/dht"
	"github.com/anacrolix/missinggo/filecache"
	"github.com/anacrolix/missinggo/x"
	"github.com/anacrolix/tagflag"
	"github.com/anacrolix/torrent"
	"github.com/anacrolix/torrent/iplist"
	"github.com/anacrolix/torrent/storage"
	"github.com/jackpal/gateway"
	"golang.org/x/time/rate"

	"streamy/core"
)

var version = ""
var commitHash = ""
var branch = ""
var buildTime = ""

var flags = struct {
	Addr          string        `help:"HTTP listen address"`
	DHTPublicIP   net.IP        `help:"IP as it will appear to the DHT network"`
	CacheCapacity tagflag.Bytes `help:"Data cache capacity (default 4GB)"`
	TorrentGrace  time.Duration `help:"How long to wait to drop a torrent after its last request (default 10m)"`
	FileDir       string        `help:"File-based storage directory, overrides piece storage"`
	Seed          bool          `help:"Seed data"`
	Debug         bool          `help:"Verbose output"`
	UseNATPMP     bool          `help:"Use NATPMP"`
}{
	Addr:          "0.0.0.0:9092",
	CacheCapacity: (10 << 30) * 4,
	TorrentGrace:  time.Minute * 2,
	UseNATPMP:     true,
}

func newTorrentClient() (ret *torrent.Client, err error) {

	blocklist, err := iplist.MMapPacked("packed-blocklist")
	if err != nil {
		log.Print(err)
	}
	storage := func() storage.ClientImpl {
		if flags.FileDir != "" {
			return storage.NewFile(flags.FileDir)
		}
		fc, err := filecache.NewCache("filecache")
		x.Pie(err)
		fc.SetCapacity(flags.CacheCapacity.Int64())
		storageProvider := fc.AsResourceProvider()
		return storage.NewResourcePieces(storageProvider)
	}()
	return torrent.NewClient(&torrent.Config{
		IPBlocklist:    blocklist,
		DefaultStorage: storage,
		DHTConfig: dht.ServerConfig{
			PublicIP:      flags.DHTPublicIP,
			StartingNodes: dht.GlobalBootstrapAddrs,
		},
		UploadRateLimiter: rate.NewLimiter(rate.Limit(1024*30), 10<<8),
		Seed:              flags.Seed,
		Debug:             flags.Debug,
		DisableIPv6:       true,
		ListenAddr:        ":53008",
	})
}

func main() {
	log.SetFlags(log.Flags() | log.Lshortfile)
	tagflag.Description(fmt.Sprintf("Streamy %s built at %s from commit %s@%s", version, buildTime, commitHash, branch))
	tagflag.Parse(&flags)
	cl, err := newTorrentClient()
	if err != nil {
		log.Fatalf("error creating torrent client: %s", err)
	}

	if nat, err := core.Discover(); err == nil {
		nat.AddPortMapping("tcp", 53008, 53008, "Streamy port TCP", 360000)
		nat.AddPortMapping("udp", 53008, 53008, "Streamy port TCP", 360000)
		defer nat.DeletePortMapping("tcp", 53008, 53008)
		defer nat.DeletePortMapping("udp", 53008, 53008)
	} else {
		var gatewayIP net.IP

		log.Printf("useNATPMP but gateway not provided, trying discovery")
		gatewayIP, err = gateway.DiscoverGateway()
		if err != nil {
			return
		}
		log.Printf("...discovered gateway IP: %s", gatewayIP)

		log.Println("Using NAT-PMP to open port.")
		if gatewayIP != nil {
			nat := core.NewNatPMP(gatewayIP)
			nat.AddPortMapping("tcp", 53008, 53008, "Streamy port TCP", 360000)
			nat.AddPortMapping("udp", 53008, 53008, "Streamy port TCP", 360000)
			defer nat.DeletePortMapping("tcp", 53008, 53008)
			defer nat.DeletePortMapping("udp", 53008, 53008)

		}

	}

	defer cl.Close()

	l, err := net.Listen("tcp4", flags.Addr)
	if err != nil {
		log.Fatal(err)
	}
	defer l.Close()
	log.Printf("serving http at %s", l.Addr())
	h := &core.Handler{cl, flags.TorrentGrace}

	err = http.Serve(l, h)
	if err != nil {
		log.Fatal(err)
	}
}
