-- El nombre que el PDF imprime en cada paso, guardado con su firmante esperado al generar el
-- documento (F14-H03, D-104): con él se compara el CN de la firma. Nulo en los firmantes de los
-- documentos generados antes, que no aceptan firmas. El rol del proceso sigue sin UPDATE (V3).
alter table firmante_esperado add column nombre varchar(200);
