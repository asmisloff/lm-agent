Документировать API для Swagger.
Использовать аннотации @ApiResponse, @Schema и подобные.

- Получить РЗ права по ID.
- Коды 200, 201, 400, 500
``` Java
@PostMapping("/right-record/{id}")
ResponseEntity<RightRecordDto> getRightRecord(
    @RequestParam(default="33") Long id
)
```