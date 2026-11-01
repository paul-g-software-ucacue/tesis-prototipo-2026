// Caddy 2.11.7 con sus módulos estándar, compilado con las dependencias de go.mod (D-80). Es el mismo
// programa que trae la imagen oficial, que no se puede usar porque su binario tiene vulnerabilidades
// ya corregidas en Go, x/net y go-jose.
package main

import (
	caddycmd "github.com/caddyserver/caddy/v2/cmd"

	_ "github.com/caddyserver/caddy/v2/modules/standard"
)

func main() {
	caddycmd.Main()
}
