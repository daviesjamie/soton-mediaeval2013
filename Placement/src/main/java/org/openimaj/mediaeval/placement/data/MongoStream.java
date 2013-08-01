package org.openimaj.mediaeval.placement.data;

import org.openimaj.util.stream.AbstractStream;

import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;


public class MongoStream extends AbstractStream<DBObject>{

    private DBCursor cursor;
    private int resultCount;
    
    public MongoStream( DBObject query, DBCollection coll ) {
        cursor = coll.find( query );
        resultCount = cursor.count();
    }
    
    @Override
    public boolean hasNext() {
        return cursor.hasNext(); 
    }

    @Override
    public DBObject next() {
        return cursor.next();
    }
    
    public int getNumResults() {
        return resultCount;
    }

}
