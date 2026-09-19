import com.neuralterrain.model.JsonTerrainModel;
import java.nio.file.Path;
var model = JsonTerrainModel.load(Path.of("run/config/neuralterrain-model.json"));
System.out.println(model.name());
System.out.println(model.predict(123456789L, 0, 0, -64, 320));
System.out.println(model.predict(987654321L, 512, 512, -64, 320));
/exit
