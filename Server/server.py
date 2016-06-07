from SocketServer import TCPServer, ThreadingMixIn, StreamRequestHandler
from Queue import Queue
import signal, ast, threading, time

class ThreadingTCPServer(ThreadingMixIn, TCPServer):
	
	def __init__(self, inet, request_handler):
		TCPServer.__init__(self, inet, request_handler)
		self.clients = []
		self.t1 = threading.Thread(target=self.broadcast_message, args=())
		self.t1.daemon = True
		self.t1.start()
		self.t2 = threading.Thread(target=self.process_usernames, args=())
		self.t2.daemon = True
		self.t2.start()
		self.t3 = threading.Thread(target=self.broadcast_usernames, args=())
		self.t3.daemon = True
		self.t3.start()
		self.lock = threading.Lock()

	def process_request(self, request, client_address):
		print client_address[0] + " connected"
		ThreadingMixIn.process_request(self, request, client_address)

	def shutdown_request(self, request):
		self.lock.acquire()
		temp = []
		for x in self.clients:
			temp.append(x[0])
		try:
			del self.clients[temp.index(request)]
		except:
			print "username was never made"
		self.lock.release()

	def broadcast_usernames(self):
		while True:
			time.sleep(5)
			load = ""
			for client in self.clients:
				load += client[1].username + ","
			load = load[:len(load)-1]
			self.lock.acquire()
			for client in self.clients:
				try:
					client[0].sendall("{\"username\":\"" + "update_usernames" + "\",\"message\":\"" + load + "\", \'code\':\'2\'}\n")
				except:
					pass
			self.lock.release()

	def process_usernames(self):
		while True:
			client = username_queue.get()
			self.lock.acquire()
			self.clients.append([client[0].request, client[0]])
			username_queue.task_done()
			self.lock.release()
			
	def broadcast_message(self):
		while True:
			payload = queue.get()
			self.lock.acquire()
			if payload[0] == 0:
				for client in self.clients:
					try:
						client[0].sendall("{\"username\":\"" + payload[2] + "\",\"message\":\"" + payload[1] + "\", \'code\':\'0\'}\n")
					except:
						pass
			queue.task_done()
			self.lock.release()

class RequestHandler(StreamRequestHandler):

	def handle(self):
		self.clientip = str(self.client_address[0])
		self.clientsocket = self.client_address[1]

		data = "default"
		while True:
			try:
				data = self.rfile.readline().strip()
			except:
				print "read error"
				break
			data = data.strip('\n')
			print self.clientip + ": " + str(data)
			if data != "" and data != None:
				try:
					payload = ast.literal_eval(data)
				except:
					print "json parse error"
					break
			else:
				break

			#code 0 = broadcast chat message
			#code 1 = set client username
			#code 2 = broadcast all connected clients
			#code 3 = disconnect

			if payload['code'] == '0':
				queue.put([0, payload['message'], self.username])
			elif payload['code'] == '1':
				self.username = payload['message']
				username_queue.put([self, self.username])
			elif payload['code'] == '2':
				queue.put([2, "default", self.username])
			elif payload['code'] == '3':
				self.wfile.write("EXIT\n")
				self.wfile.flush()
				break

	def finish(self):
		self.request.close()
		try:
			self.wfile.flush()
		except:
			pass
		self.wfile.close()
		self.rfile.close()
		print self.clientip + " disconnected"
		return
		
if __name__ == '__main__':
	PORT = int(raw_input("port: "))
	signal.signal(signal.SIGINT, signal.SIG_DFL)
	queue = Queue()
	username_queue = Queue()
	print 'Starting Server on port: ' + str(PORT)
	server = ThreadingTCPServer(("", PORT), RequestHandler)
	server.allow_reuse_address = True
	server.set_daemon = True
	server.serve_forever()
