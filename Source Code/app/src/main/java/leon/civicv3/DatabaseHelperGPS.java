package leon.civicv3;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by leonc on 2017/11/24.
 */

public class DatabaseHelperGPS extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "civicgps.db";
    public static final String TABLE_NAME = "civicgps_table";
    public static final String COL_1 = "ID";
    public static final String COL_2 = "GPS_Lat";
    public static final String COL_3 = "GPS_Long";
    public static final String COL_4 = "Time";


    public DatabaseHelperGPS(Context context) {
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("create table " + TABLE_NAME +" (ID INTEGER PRIMARY KEY AUTOINCREMENT,GPS_Lat DOUBLE,GPS_Long DOUBLE,Time STRING)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS "+TABLE_NAME);
        onCreate(db);
    }

    public boolean insertGPS(Double GPS_Lat,Double GPS_Long,String Time) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentValues = new ContentValues();
        contentValues.put(COL_2,GPS_Lat);
        contentValues.put(COL_3,GPS_Long);
        contentValues.put(COL_4,Time);
        long result = db.insert(TABLE_NAME,null ,contentValues);
        if(result == -1)
            return false;
        else
            return true;
    }

    public Cursor getAllGPS() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor resgps = db.rawQuery("select * from "+TABLE_NAME,null);
        return resgps;
    }

    public Integer deleteGPS (String id) {
        SQLiteDatabase db = this.getWritableDatabase();
        return db.delete(TABLE_NAME, "ID = ?",new String[] {id});
    }
}




