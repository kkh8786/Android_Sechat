package net.stacksmashing.sechat.db;

import android.content.ContentProviderOperation;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.CursorLoader;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.util.Log;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DAO<T extends Entity> {
    private static final Map<Class<?>, Boolean> DAOS = new HashMap<>();

    private enum Type {
        BLOB("BLOB", byte[].class) {
            @Override
            void setValueFromCursor(Object object, Field field, Cursor cursor, int columnIndex, boolean isNull) throws IllegalAccessException {
                field.set(object, isNull ? null : cursor.getBlob(columnIndex));
            }

            @Override
            void setObjectToContentValues(Object object, Field field, ContentValues values, String columnName) throws IllegalAccessException {
                values.put(columnName, (byte[]) field.get(object));
            }

            @Override
            public boolean equals(Field field, Object a, Object b) throws IllegalAccessException {
                return Arrays.equals((byte[]) field.get(a), (byte[]) field.get(b));
            }
        },

        BOOLEAN("INTEGER", boolean.class) {
            @Override
            void setValueFromCursor(Object object, Field field, Cursor cursor, int columnIndex, boolean isNull) throws IllegalAccessException {
                field.setBoolean(object, cursor.getInt(columnIndex) != 0);
            }

            @Override
            void setValueToContentValues(Object object, Field field, ContentValues values, String columnName, boolean withNulls) throws IllegalAccessException {
                values.put(columnName, field.getBoolean(object));
            }

            @Override
            public boolean equals(Field field, Object a, Object b) throws IllegalAccessException {
                return field.getBoolean(a) == field.getBoolean(b);
            }
        },

        BOOLEAN_OBJECT("INTEGER", Boolean.class) {
            @Override
            void setValueFromCursor(Object object, Field field, Cursor cursor, int columnIndex, boolean isNull) throws IllegalAccessException {
                field.set(object, isNull ? null : cursor.getInt(columnIndex) != 0);
            }

            @Override
            void setObjectToContentValues(Object object, Field field, ContentValues values, String columnName) throws IllegalAccessException {
                values.put(columnName, (Boolean) field.get(object));
            }
        },

        DOUBLE("REAL", double.class) {
            @Override
            void setValueFromCursor(Object object, Field field, Cursor cursor, int columnIndex, boolean isNull) throws IllegalAccessException {
                field.setDouble(object, cursor.getDouble(columnIndex));
            }

            @Override
            void setValueToContentValues(Object object, Field field, ContentValues values, String columnName, boolean withNulls) throws IllegalAccessException {
                values.put(columnName, field.getDouble(object));
            }

            @Override
            boolean equals(Field field, Object a, Object b) throws IllegalAccessException {
                return field.getDouble(a) == field.getDouble(b);
            }
        },

        DOUBLE_OBJECT("REAL", Double.class) {
            @Override
            void setValueFromCursor(Object object, Field field, Cursor cursor, int columnIndex, boolean isNull) throws IllegalAccessException {
                field.set(object, isNull ? null : cursor.getDouble(columnIndex));
            }

            @Override
            void setObjectToContentValues(Object object, Field field, ContentValues values, String columnName) throws IllegalAccessException {
                values.put(columnName, (Double) field.get(object));
            }
        },

        FLOAT("REAL", float.class) {
            @Override
            void setValueFromCursor(Object object, Field field, Cursor cursor, int columnIndex, boolean isNull) throws IllegalAccessException {
                field.setFloat(object, cursor.getFloat(columnIndex));
            }

            @Override
            void setValueToContentValues(Object object, Field field, ContentValues values, String columnName, boolean withNulls) throws IllegalAccessException {
                values.put(columnName, field.getFloat(object));
            }

            @Override
            boolean equals(Field field, Object a, Object b) throws IllegalAccessException {
                return field.getFloat(a) == field.getFloat(b);
            }
        },

        FLOAT_OBJECT("REAL", Float.class) {
            @Override
            void setValueFromCursor(Object object, Field field, Cursor cursor, int columnIndex, boolean isNull) throws IllegalAccessException {
                field.set(object, isNull ? null : cursor.getFloat(columnIndex));
            }

            @Override
            void setObjectToContentValues(Object object, Field field, ContentValues values, String columnName) throws IllegalAccessException {
                values.put(columnName, (Float) field.get(object));
            }
        },

        INT("INTEGER", int.class) {
            @Override
            void setValueFromCursor(Object object, Field field, Cursor cursor, int columnIndex, boolean isNull) throws IllegalAccessException {
                field.setInt(object, cursor.getInt(columnIndex));
            }

            @Override
            void setValueToContentValues(Object object, Field field, ContentValues values, String columnName, boolean withNulls) throws IllegalAccessException {
                values.put(columnName, field.getInt(object));
            }

            @Override
            boolean equals(Field field, Object a, Object b) throws IllegalAccessException {
                return field.getInt(a) == field.getInt(b);
            }
        },

        INT_OBJECT("INTEGER", Integer.class) {
            @Override
            void setValueFromCursor(Object object, Field field, Cursor cursor, int columnIndex, boolean isNull) throws IllegalAccessException {
                field.set(object, isNull ? null : cursor.getInt(columnIndex));
            }

            @Override
            void setObjectToContentValues(Object object, Field field, ContentValues values, String columnName) throws IllegalAccessException {
                values.put(columnName, (Integer) field.get(object));
            }
        },

        LONG("INTEGER", long.class) {
            @Override
            void setValueFromCursor(Object object, Field field, Cursor cursor, int columnIndex, boolean isNull) throws IllegalAccessException {
                field.setLong(object, cursor.getLong(columnIndex));
            }

            @Override
            void setValueToContentValues(Object object, Field field, ContentValues values, String columnName, boolean withNulls) throws IllegalAccessException {
                values.put(columnName, field.getLong(object));
            }

            @Override
            boolean equals(Field field, Object a, Object b) throws IllegalAccessException {
                return field.getLong(a) == field.getLong(b);
            }
        },

        LONG_OBJECT("INTEGER", Long.class) {
            @Override
            void setValueFromCursor(Object object, Field field, Cursor cursor, int columnIndex, boolean isNull) throws IllegalAccessException {
                field.set(object, isNull ? null : cursor.getLong(columnIndex));
            }

            @Override
            void setObjectToContentValues(Object object, Field field, ContentValues values, String columnName) throws IllegalAccessException {
                values.put(columnName, (Long) field.get(object));
            }
        },

        SHORT("INTEGER", short.class) {
            @Override
            void setValueFromCursor(Object object, Field field, Cursor cursor, int columnIndex, boolean isNull) throws IllegalAccessException {
                field.setShort(object, cursor.getShort(columnIndex));
            }

            @Override
            void setValueToContentValues(Object object, Field field, ContentValues values, String columnName, boolean withNulls) throws IllegalAccessException {
                values.put(columnName, field.getShort(object));
            }

            @Override
            boolean equals(Field field, Object a, Object b) throws IllegalAccessException {
                return field.getShort(a) == field.getShort(b);
            }
        },

        SHORT_OBJECT("INTEGER", Short.class) {
            @Override
            void setValueFromCursor(Object object, Field field, Cursor cursor, int columnIndex, boolean isNull) throws IllegalAccessException {
                field.set(object, isNull ? null : cursor.getShort(columnIndex));
            }

            @Override
            void setObjectToContentValues(Object object, Field field, ContentValues values, String columnName) throws IllegalAccessException {
                values.put(columnName, (Short) field.get(object));
            }
        },

        STRING("TEXT", String.class) {
            @Override
            void setValueFromCursor(Object object, Field field, Cursor cursor, int columnIndex, boolean isNull) throws IllegalAccessException {
                field.set(object, isNull ? null : cursor.getString(columnIndex));
            }

            @Override
            void setObjectToContentValues(Object object, Field field, ContentValues values, String columnName) throws IllegalAccessException {
                values.put(columnName, (String) field.get(object));
            }
        },

        DATETIME("INTEGER", Date.class) {
            @Override
            void setValueFromCursor(Object object, Field field, Cursor cursor, int columnIndex, boolean isNull) throws IllegalAccessException {
                field.set(object, isNull ? null : new Date(cursor.getLong(columnIndex)));
            }

            @Override
            void setValueToContentValues(Object object, Field field, ContentValues values, String columnName, boolean withNulls) throws IllegalAccessException {
                if (field.get(object) != null) {
                    values.put(columnName, ((Date) field.get(object)).getTime());
                }
                else if (withNulls) {
                    values.put(columnName, (Long) null);
                }
            }
        },

        ENUM("TEXT", Enum.class) {
            @Override
            void setValueFromCursor(Object object, Field field, Cursor cursor, int columnIndex, boolean isNull) throws IllegalAccessException {
                field.set(object, isNull ? null : Enum.valueOf(field.getType().asSubclass(Enum.class), cursor.getString(columnIndex)));
            }

            @Override
            void setValueToContentValues(Object object, Field field, ContentValues values, String columnName, boolean withNulls) throws IllegalAccessException {
                if (field.get(object) != null) {
                    values.put(columnName, ((Enum) field.get(object)).name());
                }
                else if (withNulls) {
                    values.put(columnName, (String) null);
                }
            }
        };

        abstract void setValueFromCursor(Object object, Field field, Cursor cursor, int columnIndex, boolean isNull) throws IllegalAccessException;

        void setValueToContentValues(Object object, Field field, ContentValues values, String columnName, boolean withNulls) throws IllegalAccessException {
            if (field.get(object) != null || withNulls) {
                setObjectToContentValues(object, field, values, columnName);
            }
        }

        void setObjectToContentValues(Object object, Field field, ContentValues values, String columnName) throws IllegalAccessException {
        }

        private static final Map<Class, Type> classToType = new HashMap<>();

        static {
            for (Type value : Type.values()) {
                classToType.put(value.aClass, value);
            }
        }

        static Type getTypeForClass(Class aClass) {
            if (aClass.isEnum()) {
                return ENUM;
            }
            return classToType.get(aClass);
        }

        private final String sqlType;
        private final Class aClass;

        Type(String sqlType, Class aClass) {
            this.sqlType = sqlType;
            this.aClass = aClass;
        }

        String getSqlType() {
            return sqlType;
        }

        boolean isNotNull() {
            return aClass.isPrimitive();
        }

        boolean equals(Field field, Object a, Object b) throws IllegalAccessException {
            Object o1 = field.get(a);
            Object o2 = field.get(b);
            return o1 == null ? o2 == null : o1.equals(o2);
        }
    }

    private final Map<Field, String> fieldToColumn = new HashMap<>();
    private final Map<String, Field> columnToField = new HashMap<>();
    private final Map<String, Type> columnTypes = new HashMap<>();
    private final Map<Field, Type> fieldTypes = new HashMap<>();
    private final List<String> columns = new ArrayList<>();

    private final Class<T> aClass;
    private final Field primaryKeyField;
    private final Uri contentUri;
    private final String tableName;
    private final UniqueConstraint[] uniqueConstraints;

    private final Map<Long, T> queryByIdCache = new HashMap<>();

    private ContentObserver contentObserver;

    public DAO(Class<T> aClass) {
        if (DAOS.containsKey(aClass)) {
            throw new IllegalStateException("More than one DAO for class " + aClass.getSimpleName());
        }
        DAOS.put(aClass, true);

        this.aClass = aClass;
        tableName = aClass.getAnnotation(Table.class).name();
        uniqueConstraints = aClass.getAnnotation(Table.class).uniqueConstraints();
        contentUri = DatabaseProvider.tableUri(tableName);
        Field primaryKeyField = null;

        for (Field field : aClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(Column.class)) {
                Column annotation = field.getAnnotation(Column.class);
                String columnName = annotation.name();
                if (columnName.length() == 0) {
                    columnName = field.getName().replaceAll("([A-Z])", "_$1").toLowerCase();
                }

                fieldToColumn.put(field, columnName);
                columnToField.put(columnName, field);
                columnTypes.put(columnName, Type.getTypeForClass(field.getType()));
                fieldTypes.put(field, Type.getTypeForClass(field.getType()));
                columns.add(columnName);

                if (annotation.primaryKey()) {
                    primaryKeyField = field;
                }
            }
        }

        if (primaryKeyField == null) {
            throw new IllegalStateException("No primary key column in DAO.Table class " + aClass.getSimpleName());
        }
        else if (!primaryKeyField.getType().equals(long.class)) {
            throw new IllegalStateException("Primary key column in DAO.Table class " + aClass.getSimpleName() + " is not of type 'long'");
        }

        this.primaryKeyField = primaryKeyField;
    }

    public Uri getContentUri() {
        return contentUri;
    }

    public T cursorToObject(Cursor cursor) {
        if (cursor == null) {
            return null;
        }

        try {
            T object = aClass.newInstance();

            for (String column : columnToField.keySet()) {
                Field field = columnToField.get(column);
                int columnIndex = cursor.getColumnIndexOrThrow(column);
                boolean isNull = cursor.isNull(columnIndex);

                columnTypes.get(column).setValueFromCursor(object, field, cursor, columnIndex, isNull);
            }

            synchronized (queryByIdCache) {
                queryByIdCache.put(primaryKeyField.getLong(object), object);
            }

            return object.cloneWithParent();
        }
        catch (Exception e) {
            Log.d("DAO", "cursorToObject", e);
            return null;
        }
    }

    private List<Field> getChangedFields(T object, Entity parent) {
        if (parent == null) {
            throw new IllegalArgumentException("Cannot get changed fields of an orphan object");
        }

        List<Field> result = new ArrayList<>();
        StringBuilder log = new StringBuilder();

        try {
            for (Field field : fieldToColumn.keySet()) {
                if (!fieldTypes.get(field).equals(field, object, parent)) {
                    result.add(field);
                    log.append(field.getName()).append(' ');
                }
            }
        }
        catch (Exception e) {
            Log.d("DAO", "getChangedFields", e);
        }

        Log.d("DAO", "Changed fields for " + aClass.getSimpleName() + ": " + log.toString());

        return result;
    }

    public ContentValues objectToContentValues(T object, boolean withNulls) {
        try {
            final Collection<Field> fields;

            if (withNulls) {
                fields = getChangedFields(object, object.getParentEntity());
                object.setParentEntity(object.clone());
            }
            else {
                fields = fieldToColumn.keySet();
            }

            ContentValues values = new ContentValues();

            for (Field field : fields) {
                if (field.getAnnotation(Column.class).primaryKey()) {
                    continue;
                }
                String columnName = fieldToColumn.get(field);

                fieldTypes.get(field).setValueToContentValues(object, field, values, columnName, withNulls);
            }

            return values;
        }
        catch (Exception e) {
            Log.d("DAO", "objectToContentValues", e);
            return null;
        }
    }

    private String[] getColumns() {
        return columns.toArray(new String[columns.size()]);
    }

    public long insert(Context context, T o) {
        return ContentUris.parseId(context.getContentResolver().insert(contentUri, objectToContentValues(o, false)));
    }

    public int update(Context context, T o) {
        try {
            final Uri uri = ContentUris.withAppendedId(contentUri, primaryKeyField.getLong(o));
            return context.getContentResolver().update(uri, objectToContentValues(o, true), null, null);
        }
        catch (IllegalAccessException e) {
            Log.e("DAO", "Could not get primary key from object " + o, e);
            return 0;
        }
    }

    public int delete(Context context, T o) {
        try {
            final Uri uri = ContentUris.withAppendedId(contentUri, primaryKeyField.getLong(o));
            return context.getContentResolver().delete(uri, null, null);
        }
        catch (IllegalAccessException e) {
            Log.e("DAO", "Could not get primary key from object " + o, e);
            return 0;
        }
    }

    public CursorLoader getCursorLoader(Context context, String selection, String[] selectionArgs, String sortOrder) {
        return new CursorLoader(context, contentUri, getColumns(), selection, selectionArgs, sortOrder);
    }

    public List<T> query(Context context, String selection, String[] selectionArgs, String sortOrder) {
        Cursor cursor = context.getContentResolver().query(contentUri, getColumns(), selection, selectionArgs, sortOrder);
        if (cursor == null) {
            return new ArrayList<>();
        }
        List<T> result = new ArrayList<>(cursor.getCount());
        if (cursor.moveToFirst()) {
            do {
                result.add(cursorToObject(cursor));
            }
            while (cursor.moveToNext());
        }
        cursor.close();
        return result;
    }

    public T queryById(Context context, long id) {
        synchronized (queryByIdCache) {
            if (contentObserver == null) {
                contentObserver = new CacheInvalidationObserver();
                context.getContentResolver().registerContentObserver(contentUri, true, contentObserver);
            }

            if (queryByIdCache.containsKey(id)) {
                return queryByIdCache.get(id).cloneWithParent();
            }

            Cursor cursor = context.getContentResolver().query(ContentUris.withAppendedId(contentUri, id), getColumns(), null, null, null);
            if (cursor == null) {
                return null;
            }
            T object = null;
            if (cursor.moveToFirst()) {
                object = cursorToObject(cursor);
            }
            cursor.close();

            return object;
        }
    }

    private void fieldToSql(Field field, StringBuilder result) {
        result.append(fieldToColumn.get(field))
                .append(' ')
                .append(fieldTypes.get(field).getSqlType());

        Column annotation = field.getAnnotation(Column.class);
        if (annotation.primaryKey()) {
            result.append(" PRIMARY KEY AUTOINCREMENT");
        }
        if (fieldTypes.get(field).isNotNull() || annotation.notNull()) {
            result.append(" NOT NULL");
        }
        if (annotation.unique()) {
            result.append(" UNIQUE");
        }
        if (annotation.defaultValue().length() != 0) {
            result.append(" DEFAULT ").append(annotation.defaultValue());
        }
        if (annotation.references().length() != 0) {
            result.append(" REFERENCES ").append(annotation.references());
        }
    }

    public String getTableCreationSql() {
        StringBuilder result = new StringBuilder("CREATE TABLE ");
        result.append(tableName).append(" (");
        boolean appendComma = false;

        for (Field field : fieldTypes.keySet()) {
            if (appendComma) {
                result.append(", ");
            }
            appendComma = true;

            fieldToSql(field, result);
        }

        for (UniqueConstraint constraint : uniqueConstraints) {
            result.append(", UNIQUE (");
            for (String column : constraint.columnNames()) {
                result.append(column).append(", ");
            }
            result.delete(result.length() - 2, result.length());
            result.append(")");
        }

        result.append(")");
        return result.toString();
    }

    public List<String> getTableUpdateSql(int oldVersion, int newVersion) {
        List<String> result = new ArrayList<>();

        for (Field field : fieldTypes.keySet()) {
            Column annotation = field.getAnnotation(Column.class);
            int sinceVersion = annotation.sinceVersion();
            if (sinceVersion > oldVersion && sinceVersion <= newVersion) {
                StringBuilder query = new StringBuilder("ALTER TABLE ");
                query.append(tableName).append(" ADD COLUMN ");
                fieldToSql(field, query);
                result.add(query.toString());
            }
        }

        return result;
    }

    public boolean shouldCreateTable(int oldVersion, int newVersion) {
        Table annotation = aClass.getAnnotation(Table.class);
        int sinceVersion = annotation.sinceVersion();
        return sinceVersion > oldVersion && sinceVersion <= newVersion;
    }

    public ContentProviderOperation insertOperation(T o) {
        return ContentProviderOperation.newInsert(contentUri).withValues(objectToContentValues(o, false)).build();
    }

    private static final String UPDATE_SELECTION = DatabaseHelper.COLUMN_ID + " = ?";

    public ContentProviderOperation updateOperation(T o) {
        String id;
        try {
            id = String.valueOf(primaryKeyField.getLong(o));
        }
        catch (IllegalAccessException e) {
            id = "-1";
        }

        return ContentProviderOperation.newUpdate(contentUri)
                .withSelection(UPDATE_SELECTION, new String[]{id})
                .withValues(objectToContentValues(o, true))
                .build();
    }

    private class CacheInvalidationObserver extends ContentObserver {
        public CacheInvalidationObserver() {
            super(null);
        }

        @Override
        public boolean deliverSelfNotifications() {
            return true;
        }

        @Override
        public void onChange(boolean selfChange) {
            onChange(selfChange, null);
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            synchronized (queryByIdCache) {
                queryByIdCache.clear();
            }
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @interface Column {
        String name() default "";

        boolean primaryKey() default false;

        boolean notNull() default false;

        boolean unique() default false;

        String defaultValue() default "";

        String references() default "";

        int sinceVersion() default 1;
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Table {
        String name();

        int sinceVersion() default 1;

        UniqueConstraint[] uniqueConstraints() default {};
    }

    @Retention(RetentionPolicy.RUNTIME)
    @interface UniqueConstraint {
        String[] columnNames();
    }
}
