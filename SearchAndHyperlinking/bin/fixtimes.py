import sys
import os 

def fixtime(splits,index):
	splits[index] = "%2.2f"%(float(splits[index]))

for x in sys.argv[1:]:
	f = file(x)
	bn = os.path.basename(x)
	tout = file(bn,"w")
	for l in f.readlines():
		line = l.strip()
		if len(line) == 0: continue
		linesplit = line.split(" ")
		fixtime(linesplit,3)
		fixtime(linesplit,4)
		fixtime(linesplit,5)
		newline = " ".join(linesplit)
		tout.write("%s\n"%newline)
	tout.close()
