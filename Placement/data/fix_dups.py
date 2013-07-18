import simplejson

# Script expects geoJSON file to be in format:
# { "code": "AFG", "name": "Afghanistan", "geometry": { "type": "Polygon", "coordinates": [[...]] } },

TARGETS = ['ATA','CAN','USA']

with open('countries.geo.json') as f:
    fc = simplejson.load(f)

for country in fc:
    if country['code'] in TARGETS:
        dups = []
        for multipoly in country['geometry']['coordinates']:
            for poly in multipoly:
                tmp = []
                for coord in poly:
                    if coord not in tmp:
                        tmp.append(coord)
                    else:
                        if coord == tmp[0] and tmp[-1] == poly[-2]:
                            tmp.append(coord)
                        else:
                            dups.append(coord)
        if len(dups) > 0:
            print country['name'] + ' (' + country['code'] + ') duplicates:'
            for dup in dups:
                print dup

