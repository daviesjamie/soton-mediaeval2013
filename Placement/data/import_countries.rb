require "rexml/document"

# Open the file as an XML document
file = File.new("countries_world.kml")
doc = REXML::Document.new file

# Get all placemark elements as an array.
all_pms = doc.elements.to_a("//Placemark")

# Create SQL file with all the insert statements
f = File.open "import_countries.sql", "w+"

# Iterate over each Placemark element
all_pms.each do |pm|
    # The name of each polygon comes from the child element name
    pc_name = pm.elements['name'].text

    # Polygon points come in 1 string
    raw_polygons = pm.elements.to_a('MultiGeometry/Polygon/outerBoundaryIs/LinearRing/coordinates')

    # For each defined polygon
    clean_polygons = Array.new
    raw_polygons.each do |rp|

        # Format data for MySQL
        pc_polygon =  rp.text.strip.gsub(' ','#').gsub(',',' ').gsub('#',',').split(',').collect!{|p| p.split.reverse!.join(' ')}.join(', ')
        clean_polygons.push "((#{pc_polygon}))"
        
    end

    # Concat all this nicely into 1 executable INSERT statement
    f << "INSERT INTO country_polygons (countryName, polygon) VALUES ('#{pc_name}',GeomFromText('MultiPolygon(#{clean_polygons.join(', ')})'));\n"
end

# Close the stream
f.flush
f.close
