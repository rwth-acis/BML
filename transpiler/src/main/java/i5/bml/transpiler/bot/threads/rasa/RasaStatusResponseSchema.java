package i5.bml.transpiler.bot.threads.rasa;

import com.google.gson.annotations.SerializedName;

public record RasaStatusResponseSchema(@SerializedName("model_id") String modelId, @SerializedName("model_file") String modelFile,
                                       @SerializedName("num_active_training_jobs") int activeTrainingJobs) {}
