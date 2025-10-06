package xyz.jonesdev.sonar.api.database.ormlite;

import com.j256.ormlite.jdbc.db.PostgresDatabaseType;
import org.jetbrains.annotations.NotNull;

public final class PostgresDatabaseTypeAdapter extends PostgresDatabaseType {

  // We need to override the default driver class name
  // to use the custom relocated MySQL driver
  @Override
  protected String @NotNull [] getDriverClassNames() {
     return new String[]{"xyz.jonesdev.sonar.libs.postgresql.Driver"};
  }
}
