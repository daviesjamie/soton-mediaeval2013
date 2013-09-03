#!/usr/bin/env python
from pymongo import MongoClient
from IPython import embed
import pprint
pp = pprint.PrettyPrinter(indent=4)
mc = MongoClient(host="seurat")
training = mc.mediaeval.training

def matkeys(pre="details."):
	ret = dict([(x,"$%s%s"%(pre,x)) for x in training.find_one()["details"].keys() if "mat" in x])
	return ret
def matkeysavg():
	ret = dict([(x,{"$avg":"$_id.%s"%(x)}) for x in training.find_one()["details"].keys() if "mat" in x])
	return ret
# print dict({"_id":"avg"}.items() + matkeysavg().items())
results = training.aggregate([
	{
		"$group":{
			"_id":dict(matkeys().items() + {"eps":"$details.eps"}.items()),
			"f1avg":{"$avg":"$scores.f1"}
		}
	},
	# {
	# 	"$group":{
	# 		"_id":{"eps":"$details.eps"},
	# 		"f1avg":{"$avg":"$scores.f1"}
	# 	}
	# },
	{
		"$sort":{
			"f1avg":-1
		}
	},
	# {
	# 	"$group":dict({
	# 		"_id":"avg"
	# 	}.items() + matkeysavg().items())
	# }
	{
		"$limit":100

	},
	{
		"$project":dict(
			matkeys("_id.").items() +
			{"f1avg":1,"eps":"$_id.eps","_id":0}.items()
		)
	}
])['result']

for r in results:
	pp.pprint(r)
