package at.pegelhub.watchdog;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.time.Instant;

/** Shared state and diagnostic encoding; instants use ISO-8601 strings rather than JVM object details. */
public final class Json {
    public static final Gson CODEC = new GsonBuilder()
            .registerTypeAdapter(Instant.class, new TypeAdapter<Instant>() {
                @Override
                public void write(JsonWriter out, Instant value) throws IOException {
                    out.value(value.toString());
                }

                @Override
                public Instant read(JsonReader in) throws IOException {
                    return Instant.parse(in.nextString());
                }
            }.nullSafe())
            .create();

    private Json() {
    }
}
