package fs

import (
	"github.com/jacobsa/fuse/fuseops"
	"github.com/jacobsa/fuse/fuseutil"
)

type inodeInfo struct {
	attributes fuseops.InodeAttributes

	// File or directory?
	dir bool

	// For directories, children.
	children []fuseutil.Dirent
}
