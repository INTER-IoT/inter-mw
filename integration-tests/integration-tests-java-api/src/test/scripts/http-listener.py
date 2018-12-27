#!/usr/bin/env python
from BaseHTTPServer import BaseHTTPRequestHandler, HTTPServer
import SocketServer

class S(BaseHTTPRequestHandler):
    def _set_headers(self):
        self.send_response(204)
        self.end_headers()

    def do_POST(self):
        content_length = int(self.headers['Content-Length'])
        post_data = self.rfile.read(content_length)
        self._set_headers()
        print post_data
        print "-----------------------------------------------------------------"

def run(port, server_class=HTTPServer, handler_class=S):
    server_address = ('', port)
    httpd = server_class(server_address, handler_class)
    print 'Listening on port {}...'.format(port)
    httpd.serve_forever()

if __name__ == "__main__":
    from sys import argv

    if len(argv) == 2:
        run(port=int(argv[1]))
    else:
        print "Usage: http-listener.py <port>"
