#!/usr/bin/env python
import sys
import re
import os
import mmap
from cPickle import *
from pymongo import MongoClient



expregex =\
'(?:(?:([^/=]*)|([^=]*?)=([^/]*))/)'
expregex = re.compile(expregex)
f1regex =\
'f1score \| s=[(]f1=([^)]*)[)],b=([^,]*),d=([^ ]*)'

f1regex = re.compile(f1regex)
decregex =\
"decision \| [{](.*)[}]"
decregex = re.compile(decregex)
def _parseexp(filename):
	fnparts = re.findall(expregex,filename)
	foundstart = False
	exp = {}
	for x in fnparts:

		if not foundstart:
			if x[1] == "inc":
				foundstart = True
			continue
		if len(x[0]) > 0:
			if "_" in x[0]:
				start,stop = x[0].split("_")
				exp["start"] = start
				exp["stop"] = stop
			else:
				exp[x[0]] = True
		else:
			key = x[1]
			key = key.replace(".","_")
			if key == "nac":
				exp[key] = bool(x[2])
			elif key == "combine":
				exp[key] = x[2]
			else:
				exp[key] = float(x[2])
	return exp

outcoll = sys.argv[1]
mc = MongoClient()
memongo = mc.mediaeval
coll = memongo[outcoll]
coll.drop()


exps = []
for fn in sys.stdin:
	fn = fn.strip()
	exp = {}
	exp["details"] = _parseexp(fn)

	size = os.stat(fn).st_size
	f = open(fn)
	data = mmap.mmap(f.fileno(), size, access=mmap.ACCESS_READ)
	m = re.search(f1regex, data)
	f1,b,d = m.groups()
	exp["scores"] = {}
	exp["scores"]["f1"] = float(f1)
	exp["scores"]["randomf1"] = float(b)
	exp["scores"]["correctf1"] = float(d)
	decdict = dict([x.split("=") for x in re.search(decregex,data).groups()[0].split(",")])
	for x in decdict:
		exp["scores"][x] = float(decdict[x])
	coll.insert(exp)
	
coll.create_index("f1",-1)