from pylab import *
import scipy.sparse as ssp
import sys
import cPickle as pickle

outf = file(sys.argv[3],"w")
f = open(sys.argv[1])
ftoind = {}
for line in f:
	line = line.split(" ")
	ftoind[line[0]] = int(line[1])
f = open(sys.argv[2])
smat = ssp.lil_matrix((len(ftoind),len(ftoind)))

seen = 0
for line in f:
	if seen % 1000 == 0:
		print "Seen:",seen
	seen+=1
	imgs,value =  line.strip().split("\t")
	value = float(value)
	img1,img2 = imgs.split(" ")
	img1 = img1.split("/")[2].split(".")[0]
	img2 = img2.split("/")[2].split(".")[0]
	ind1 = ftoind[img1]
	ind2 = ftoind[img2]
	smat[ind1,ind2] = value
	smat[ind2,ind1] = value


pickle.dump(smat,outf)
