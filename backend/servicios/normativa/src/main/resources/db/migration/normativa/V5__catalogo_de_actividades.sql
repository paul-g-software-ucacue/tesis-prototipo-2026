-- El catálogo de actividades de los arts. 140 a 143 del RCE (F06-H04, RF-21, RN-22, D-79), con el
-- texto de la política de asignación de carga horaria de marzo de 2024. Una fila por literal concreto.
-- No entran 140 a), impartir clases, que son las propuestas; 143 i), comisiones (C-12); ni los
-- literales «Las demás que defina…» (140 p, 141 j, 142 j y 143 q), porque RN-22 solo admite las
-- contempladas. Lo que exige cada actividad también es dato: tipos_de_proyecto (RN-25) y cargos.
-- Se agrega a cada versión, aunque rija un periodo, con su registro: completa la versión, no cambia
-- un valor (D-68). Una versión nueva copia el de la anterior (RepositorioVersionesJdbc).

create table actividad_catalogo (
    -- En cascada: el catálogo es parte de la versión. Las versiones no se borran en la aplicación; así
    -- los scripts de deploy/ limpian la versión de prueba sin conocer esta tabla.
    version_id uuid not null references version_normativa (id) on delete cascade,
    codigo varchar(6) not null check (codigo ~ '^14[0-3]-[a-p]$'),
    orden int not null check (orden >= 1),
    componente varchar(14) not null
        check (componente in ('OTRAS_DOCENCIA', 'INVESTIGACION', 'VINCULACION', 'GESTION')),
    nombre varchar(500) not null,
    tipos_de_proyecto text[] not null default '{}',
    cargos text[] not null default '{}',
    primary key (version_id, codigo),
    unique (version_id, orden),
    -- Una actividad exige un proyecto o un cargo, nunca los dos.
    check (cardinality(tipos_de_proyecto) = 0 or cardinality(cargos) = 0)
);

create temporary table actividad_por_defecto (
    orden int, codigo varchar(6), componente varchar(14), nombre varchar(500),
    tipos text[], cargos text[]) on commit drop;

insert into actividad_por_defecto
select fila.orden, fila.codigo, 'OTRAS_DOCENCIA', fila.nombre,
    case when fila.codigo = '140-f'
        then array['PRACTICAS_PREPROFESIONALES', 'SERVICIO_COMUNITARIO'] else array[]::text[] end,
    array[]::text[]
from (values
    (1, '140-b', 'Planificar y actualizar contenidos de clases, seminarios, talleres, entre otros'),
    (2, '140-c', 'Adaptaciones curriculares'),
    (3, '140-d', 'Diseñar y elaborar recursos didácticos, guías de estudio, sílabos, entre otros'),
    (4, '140-e', 'Orientar y acompañar a estudiantes a través de tutorías presenciales o virtuales, individuales o grupales'),
    (5, '140-f', 'Dirección, tutorías, seguimiento y evaluación de las actividades académicas y/o educativas en los ambientes reales de la formación del estudiante en todas sus modalidades de formación'),
    (6, '140-g', 'Preparar, elaborar, aplicar, calificar y registrar exámenes, trabajos, proyectos y prácticas'),
    (7, '140-h', 'Dirección, tutoría, lectoría y/o calificación de trabajos de titulación'),
    (8, '140-i', 'Dirigir y participar en proyectos de experimentación e innovación para el perfeccionamiento docente'),
    (9, '140-j', 'Diseñar e impartir cursos de educación continua, de capacitación o actualización'),
    (10, '140-k', 'Participar en actividades de proyectos sociales, artísticos, culturales, productivos y empresariales de vinculación con la sociedad articulados a la docencia e innovación educativa'),
    (11, '140-l', 'Participar y organizar colectivos académicos de debate, capacitación o intercambio de metodologías y experiencias de enseñanza'),
    (12, '140-m', 'Desarrollar, incorporar y sistematizar la investigación formativa como eje transversal del proceso de enseñanza - aprendizaje'),
    (13, '140-n', 'Impartir cursos de inducción a nuevos estudiantes'),
    (14, '140-o', 'Orientar, capacitar y acompañar al personal académico del Sistema Nacional de Nivelación y Admisión')
) as fila (orden, codigo, nombre);

insert into actividad_por_defecto
select fila.orden, fila.codigo, 'INVESTIGACION', fila.nombre,
    array['INVESTIGACION'], array[]::text[]
from (values
    (15, '141-a', 'Diseñar, dirigir y ejecutar proyectos de investigación aplicada, científica o tecnológica, que supongan creación, innovación, difusión, transferencia de los resultados obtenidos e inserción de los conocimientos en la práctica social'),
    (16, '141-b', 'Realizar investigación para la comprensión, recuperación, fortalecimiento y potenciación de los saberes ancestrales'),
    (17, '141-c', 'Diseñar, elaborar, ejecutar y evaluar las metodologías, instrumentos, protocolos, procedimientos operativos o de investigación'),
    (18, '141-d', 'Investigar en laboratorios, centros documentales y demás instalaciones habilitadas para esta función, así como en entornos sociales, naturales y/o virtuales'),
    (19, '141-e', 'Participar en congresos, seminarios y conferencias para la presentación de avances y/o resultados de sus investigaciones'),
    (20, '141-f', 'Diseñar, gestionar y participar en redes y programas de investigación local, nacional e internacional'),
    (21, '141-g', 'Participar en comités o consejos académicos y editoriales de revistas científicas y académicas indexadas, internas y/o externas'),
    (22, '141-h', 'Difundir los resultados y beneficios sociales de la investigación, a través de publicaciones, producciones artísticas, eventos, actuaciones, conciertos, creación u organización de instalaciones y de exposiciones, entre otros'),
    (23, '141-i', 'Dirigir o participar en colectivos académicos de debate para la presentación de avances y resultados de investigaciones, innovaciones y desarrollos tecnológicos')
) as fila (orden, codigo, nombre);

insert into actividad_por_defecto
select fila.orden, fila.codigo, 'VINCULACION', fila.nombre,
    array['VINCULACION', 'PRACTICAS_PREPROFESIONALES', 'SERVICIO_COMUNITARIO'], array[]::text[]
from (values
    (24, '142-a', 'Elaborar, coordinar y desarrollar programas, proyectos y/o actividades de vinculación e innovación que permitan aplicar los conocimientos generados en las instituciones de educación superior en la solución de problemáticas específicas y de interés público que generen valor agregado e impacto favorable en los procesos productivos'),
    (25, '142-b', 'Elaborar, coordinar y desarrollar programas, proyectos y/o actividades de transferencia, democratización del conocimiento e innovación social'),
    (26, '142-c', 'Prestar asistencia técnica, de servicios especializados que generen beneficio a la colectividad y a las instituciones de educación superior'),
    (27, '142-d', 'Participar en los procesos de educación continua, capacitación y actualización de conocimientos, y certificación de competencias'),
    (28, '142-e', 'Prestar servicios a la sociedad que no generen beneficio económico para el instituto o conservatorio para su personal académico, tales como el análisis de laboratorio especializado, el peritaje judicial, la revisión técnica documental para las instituciones del Estado, entre otras. La participación en trabajos de consultoría institucional no se reconocerá como actividad de vinculación dentro de la dedicación horaria'),
    (29, '142-f', 'Participar en los procesos del Organismo Evaluador de la Conformidad (OEC) y Operadores de Capacitación Calificados (OCC)'),
    (30, '142-g', 'Fomentar la constitución, desarrollo y fortalecimiento de organizaciones de la sociedad civil, redes y demás espacios de participación ciudadana'),
    (31, '142-h', 'Organizar o participar en actividades de divulgación, democratización y distribución del saber'),
    (32, '142-i', 'Conformar y participar en redes y programas de vinculación con la sociedad local, nacional e internacional')
) as fila (orden, codigo, nombre);

insert into actividad_por_defecto
select fila.orden, fila.codigo, 'GESTION', fila.nombre, array[]::text[], fila.cargos
from (values
    (33, '143-a', 'Desempeñar funciones de rector, vicerrector o integrante del Órgano Colegiado Superior de las Instituciones de Formación Técnica y Tecnológica Públicas y Conservatorios Superiores Públicos, y/o del Órgano de Consulta de la formación técnica y tecnológica', array['RECTOR', 'VICERRECTOR']),
    (34, '143-b', 'Coordinación Académica', array['COORDINADOR_ACADEMICO_INSTITUCIONAL']),
    (35, '143-c', 'Coordinar sede, campus y/o extensión', array['COORDINADOR_SEDE']),
    (36, '143-d', 'Coordinar carreras o programas', array['COORDINADOR_CARRERA']),
    (37, '143-e', 'Coordinar la investigación', array['G07_GESTOR_INVESTIGACION']),
    (38, '143-f', 'Coordinar la vinculación con la sociedad', array['G06_GESTOR_VINCULACION']),
    (39, '143-g', 'Coordinar el centro de idiomas', array['G08_GESTOR_IDIOMAS']),
    (40, '143-h', 'Coordinar el centro de formación integral y servicios especializados', array['G09_GESTOR_FORMACION_INTEGRAL']),
    (41, '143-j', 'Desempeñar en encargo de funciones o responsabilidades', array[]::text[]),
    (42, '143-k', 'Organizar o dirigir eventos académicos nacionales o internacionales', array[]::text[]),
    (43, '143-l', 'Gestionar procesos de internacionalización de la comunidad institucional y fomento de las relaciones internacionales', array[]::text[]),
    (44, '143-m', 'Diseñar y rediseñar las carreras y programas de tercer nivel técnico - tecnológico, tecnología superior universitaria y posgrado tecnológico', array[]::text[]),
    (45, '143-n', 'Participar como delegado institucional, en eventos de instituciones públicas y/o privadas', array[]::text[]),
    (46, '143-o', 'Participar como evaluador o facilitador académico externo del Consejo de Educación Superior, Consejo de Aseguramiento de la Calidad de la Educación Superior, el Órgano Rector de la Política Pública de Educación Superior u otro organismo público de investigación o desarrollo tecnológico', array[]::text[]),
    (47, '143-p', 'Desempeñar cargos tales como: editor académico, director o miembro editorial de una publicación', array[]::text[])
) as fila (orden, codigo, nombre, cargos);

insert into actividad_catalogo (version_id, codigo, orden, componente, nombre, tipos_de_proyecto, cargos)
select v.id, d.codigo, d.orden, d.componente, d.nombre, d.tipos, d.cargos
from version_normativa v
cross join actividad_por_defecto d;

-- Un VERSION_EDITADA de sistema por versión completada, como en V4 (D-68).
insert into registro_cambio (id, momento, usuario, accion, detalle)
select gen_random_uuid(), now(), 'sistema', 'VERSION_EDITADA',
    jsonb_build_object(
        'numero', v.numero,
        'cambios', jsonb_build_array(
            jsonb_build_object('campo', 'actividades', 'anterior', null, 'nuevo', '47')))
from version_normativa v;
