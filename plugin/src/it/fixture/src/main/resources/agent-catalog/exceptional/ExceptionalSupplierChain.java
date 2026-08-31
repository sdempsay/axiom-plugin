return load(path)
    .chain((listener, result) -> enrich(result), listener);
