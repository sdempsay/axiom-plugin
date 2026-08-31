final ExceptionalResponse<Data> response = ExceptionalSupplier.of(() -> fetchData())
    .execute();
if (response.wasError()) {
    return fallback();
}
return response.response();
