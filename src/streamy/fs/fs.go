package fs

import (
	"context"
	"encoding/binary"
	"io"
	"os"
	"strings"

	"github.com/anacrolix/torrent"

	"github.com/jacobsa/fuse"
	"github.com/jacobsa/fuse/fuseops"
	"github.com/jacobsa/fuse/fuseutil"
	"github.com/jacobsa/timeutil"
)

func NewtorrentFS(client *torrent.Client) (server fuse.Server, err error) {
	fs := &torrentFS{
		Client: client,
	}
	server = fuseutil.NewFileSystemServer(fs)
	return
}

type torrentFS struct {
	fuseutil.NotImplementedFileSystem

	Clock  timeutil.Clock
	Client *torrent.Client
}

func (fs *torrentFS) StatFS(ctx context.Context, op *fuseops.StatFSOp) (err error) {
	return
}

func findChildInode(name string, children []fuseutil.Dirent) (inode fuseops.InodeID, err error) {
	for _, e := range children {
		if e.Name == name {
			inode = e.Inode
			return
		}
	}

	err = fuse.ENOENT
	return
}

func (fs *torrentFS) allTorrents() map[fuseops.InodeID]inodeInfo {
	ls := map[fuseops.InodeID]inodeInfo{}
	torrents := []fuseutil.Dirent{}

	for i, t := range fs.Client.Torrents() {

		id := fuseops.InodeID(binary.BigEndian.Uint64(t.InfoHash().Bytes()[:]))

		torrents = append(torrents, fuseutil.Dirent{
			Offset: fuseops.DirOffset(i),
			Inode:  id,
			Name:   t.Name(),
			Type:   fuseutil.DT_Directory,
		})

		files := []fuseutil.Dirent{}
		for i, f := range t.Files() {
			fid := fuseops.InodeID(i) + id
			sl := strings.Split(f.DisplayPath(), "/")
			files = append(files, fuseutil.Dirent{
				Offset: fuseops.DirOffset(i),
				Inode:  fid,
				Name:   sl[len(sl)-1],
				Type:   fuseutil.DT_File,
			})
			ls[fid] = inodeInfo{
				attributes: fuseops.InodeAttributes{
					Nlink: 1,
					Mode:  0444,
					Size:  uint64(f.Length()),
				},
			}
		}

		ls[id] = inodeInfo{
			attributes: fuseops.InodeAttributes{
				Nlink: 1,
				Mode:  0555 | os.ModeDir,
			},
			dir:      true,
			children: files,
		}
	}

	ls[fuseops.RootInodeID] = inodeInfo{
		attributes: fuseops.InodeAttributes{
			Nlink: 1,
			Mode:  0555 | os.ModeDir,
		},
		dir:      true,
		children: torrents,
	}
	return ls
}

func (fs *torrentFS) LookUpInode(ctx context.Context, op *fuseops.LookUpInodeOp) (err error) {
	// Find the info for the parent.
	gInodeInfo := fs.allTorrents()

	parentInfo, ok := gInodeInfo[op.Parent]
	if !ok {
		err = fuse.ENOENT
		return
	}

	// Find the child within the parent.
	childInode, err := findChildInode(op.Name, parentInfo.children)
	if err != nil {
		return
	}

	// Copy over information.
	op.Entry.Child = childInode
	op.Entry.Attributes = gInodeInfo[childInode].attributes

	return
}

func (fs *torrentFS) GetInodeAttributes(ctx context.Context, op *fuseops.GetInodeAttributesOp) (err error) {
	// Find the info for this inode.
	gInodeInfo := fs.allTorrents()
	info, ok := gInodeInfo[op.Inode]
	if !ok {
		err = fuse.ENOENT
		return
	}

	// Copy over its attributes.
	op.Attributes = info.attributes

	return
}

func (fs *torrentFS) OpenDir(ctx context.Context, op *fuseops.OpenDirOp) (err error) {
	// Allow opening any directory.
	return
}

func (fs *torrentFS) ReadDir(ctx context.Context, op *fuseops.ReadDirOp) (err error) {
	// Find the info for this inode.
	gInodeInfo := fs.allTorrents()

	info, ok := gInodeInfo[op.Inode]
	if !ok {
		err = fuse.ENOENT
		return
	}

	if !info.dir {
		err = fuse.EIO
		return
	}

	entries := info.children

	// Grab the range of interest.
	if op.Offset > fuseops.DirOffset(len(entries)) {
		err = fuse.EIO
		return
	}

	entries = entries[op.Offset:]

	// Resume at the specified offset into the array.
	for _, e := range entries {
		n := fuseutil.WriteDirent(op.Dst[op.BytesRead:], e)
		if n == 0 {
			break
		}

		op.BytesRead += n
	}

	return
}

func (fs *torrentFS) OpenFile(ctx context.Context, op *fuseops.OpenFileOp) (err error) {
	// Allow opening any file.
	return
}

func (fs *torrentFS) ReadFile(ctx context.Context, op *fuseops.ReadFileOp) (err error) {
	// Let io.ReaderAt deal with the semantics.
	reader := strings.NewReader("Hello, world!")

	op.BytesRead, err = reader.ReadAt(op.Dst, op.Offset)

	// Special case: FUSE doesn't expect us to return io.EOF.
	if err == io.EOF {
		err = nil
	}

	return
}
