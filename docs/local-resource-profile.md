# Local resource profiles

These are planning guidelines, not guaranteed memory footprints. Model files on disk are smaller than total runtime RAM use; context length, concurrency, operating system, Docker overhead and CPU/GPU memory all matter. Start with the non-AI application, measure free RAM with `scripts/system-info.ps1` or `scripts/system-info.sh`, then opt into AI only if there is headroom. All three Spring Boot `bootRun` processes default to `-Xmx512m`, configurable through `TRAVEL_JVM_MAX_HEAP`, `PROPERTY_JVM_MAX_HEAP`, and `INTEGRATION_JVM_MAX_HEAP`.

| System RAM | Non-AI setup | Optional model to consider | Practical guidance |
|---|---|---|---|
| 8 GB | PostgreSQL, two bounded JVMs and Vite; close heavy apps if needed | `gemma3:1b` (~815 MB file) or `qwen3:0.6b` (~523 MB) | Use short context and one request at a time; skip local AI when free RAM is low. |
| 16 GB | Three bounded JVMs, PostgreSQL and Vite; close heavy IDEs as needed | `qwen3:4b-instruct` (~2.5 GB) or `gemma3:4b` (~3.3 GB) | This machine selected `qwen3:4b-instruct`; use short context and one request at a time. |
| 24 GB | More room for IDE, browser and local inference | `qwen3:8b` (~5.2 GB) or `gemma3:12b` (~8.1 GB) | 12B may be slow on CPU; keep context/concurrency bounded. |
| 32 GB or more | Room for larger experiments | `qwen3:14b` (~9.3 GB), then evaluate larger models such as `gemma3:27b` (~17 GB) | Choose by measured latency, output quality and GPU availability, not RAM alone. |

The model names and file sizes come from the [Ollama Qwen3 tags](https://ollama.com/library/qwen3/tags) and [Gemma 3 tags](https://ollama.com/library/gemma3/tags). They are recommendations for **manual selection**, not dependencies or automatic downloads. Check each model's license and suitability before use. For small machines, the complete non-AI application remains the supported baseline.

To opt in after Docker is running: `docker compose --profile ai up -d ollama`. This starts a version-pinned Ollama container on loopback port 11434; it does not download a model. The selected model can be pulled explicitly with `docker compose exec ollama ollama pull qwen3:4b-instruct`. Stop it with `docker compose --profile ai stop ollama`; the web and all three services continue to run. The CPU image is the default; GPU-specific Docker configuration is a later local choice. On this machine the model was pulled and smoke-tested, then the container was stopped to free RAM.
