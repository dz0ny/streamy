package core

import "expvar"

var (
	eventHandlerWebsocketReadClosed = expvar.NewInt("EventHandlerWebsocketReadClosed")
	eventHandlerContextDone         = expvar.NewInt("EventHandlerContextDone")
)
