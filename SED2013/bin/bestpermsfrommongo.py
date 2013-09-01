#/usr/bin/env python
import sys
import re
import os
import mmap
from cPickle import *
from pymongo import MongoClient
from IPython import embed
from pylab import *

expregex =\
'(?:(?:([^/=]*)|([^=]*?)=([^/]*))/)'
expregex = re.compile(expregex)

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
	return exp.items()

outcoll = sys.argv[1]
mc = MongoClient()
memongo = mc.mediaeval

featureFileOrder = None
if len(sys.argv) > 2:
	expbits = _parseexp(sys.argv[2])
	featureFileOrder = [x[0] for x in expbits if "mat" in x[0]]

traincoll = memongo.training
curs = traincoll.find()
curs = curs.sort("f1",-1)
cnext = curs.next()
props = [cnext[x] for x in featureFileOrder]
arrprops = array(props) * cnext["f1"] / sum(array(props))
seen = 1
while cnext["f1"] > 0.9:
	print ",".join([str(int(x)) for x in props])
	props = [cnext[x] for x in featureFileOrder]
	cnext = curs.next()
	arrprops = vstack([arrprops,array(props)* cnext["f1"]])

print ",".join([str(x) for x in arrprops.mean(axis=0)])