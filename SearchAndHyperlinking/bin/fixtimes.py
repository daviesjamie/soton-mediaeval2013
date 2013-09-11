import sys
import os 
import math

def fixtime(splits,index):
    splits[index] = "%2.2f"%(float(splits[index]))

def time(timestr):
    mins = math.floor(float(timestr))
    secs = (float(timestr) - mins) * 100
    return (mins * 60) + secs

def detime(timeval):
    mins = math.floor(timeval / 60)
    secs = timeval - (60 * mins)
    return "%2.2f"%(mins + (secs / 100))

f = file(sys.argv[1])
bn = os.path.basename(sys.argv[2])
tout = file(bn,"w")
for l in f.readlines():
    line = l.strip()
    if len(line) == 0: continue
    linesplit = line.split(" ")
    start = time(linesplit[3])
    end = time(linesplit[4])
    if end - start > 120:
        avg = (end - start) / 2
        start = avg - 59
        end = avg + 59
    elif end - start < 10:
        start = start - 5
        end = end + 5
    if start < 0:
        end = end - start
        start = 0
    linesplit[3] = detime(start)
    linesplit[4] = detime(end)
    newline = " ".join(linesplit)
    tout.write("%s\n"%newline)
tout.close()
