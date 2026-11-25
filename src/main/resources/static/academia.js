'use strict';

const content = document.querySelector('#contenido');
const dialog = document.querySelector('#dialog');
const notification = document.querySelector('#notification');
const navigation = document.querySelector('#navigation');
const sidebar = document.querySelector('#sidebar');
const menuToggle = document.querySelector('#menu-toggle');
const state = { account: null, csrfHeader: null, csrfToken: null, viewVersion: 0, today: null };
const roles = { ADMIN: 'Administrador', TEACHER: 'Profesor', STUDENT: 'Estudiante' };
const statuses = {
    PRESENTE: 'Presente', AUSENTE: 'Ausente', TARDANZA: 'Tardanza', JUSTIFICACION: 'Justificación'
};
const weekdays = ['Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado', 'Domingo'];
const numberFormat = new Intl.NumberFormat('es-PE', { maximumFractionDigits: 2 });
let toastTimer;

function node(tag, attrs = {}, ...children) {
    const item = document.createElement(tag);
    for (const [key, value] of Object.entries(attrs)) {
        if (value != null && value !== false) item.setAttribute(key, value === true ? '' : value);
    }
    for (const child of children.flat()) {
        if (child != null) item.append(child instanceof Node ? child : document.createTextNode(String(child)));
    }
    return item;
}

function button(label, handler, style = 'secondary') {
    const element = node('button', { class: style, type: 'button' }, label);
    element.addEventListener('click', handler);
    return element;
}

function displayDate(date) {
    return date ? date.split('-').reverse().join('/') : '—';
}

function optionList(entries) {
    return Object.entries(entries).map(([value, label]) => [value, label]);
}

function field(name, label, settings = {}) {
    const { options, placeholder, value, ...attributes } = settings;
    const attrs = { name, required: true, ...attributes };
    const input = options
        ? node('select', attrs, placeholder == null ? null : node('option', { value: '' }, placeholder),
            options.map(([key, text]) => node('option', { value: key }, text)))
        : node('input', attrs);
    if (value != null) input.value = value;
    return node('label', {}, label, input);
}

function fieldValue(form, name) {
    return form.elements.namedItem(name).value;
}

function optional(form, name) {
    const value = fieldValue(form, name);
    return value === '' ? null : value;
}

function values(form) {
    return Object.fromEntries(new FormData(form));
}

function roleIs(role) {
    return state.account.role === role;
}

function canTeach() {
    return !roleIs('STUDENT');
}

function fullName(person) {
    return person.firstName + ' ' + person.lastName;
}

function selectCourse(courses, value = '', includeAll = false) {
    return field('courseId', 'Curso', {
        options: courses.map(course => [course.id, course.code + ' · ' + course.name]),
        placeholder: includeAll ? 'Todos los cursos' : 'Selecciona un curso',
        required: !includeAll, value
    });
}

function query(parameters) {
    const search = new URLSearchParams();
    for (const [key, value] of Object.entries(parameters)) {
        if (value != null && value !== '') search.set(key, value);
    }
    return search.size ? '?' + search : '';
}

async function api(path, method = 'GET', data) {
    const headers = {};
    if (method !== 'GET') headers[state.csrfHeader] = state.csrfToken;
    if (data != null) headers['Content-Type'] = 'application/json';
    const response = await fetch('/api' + path, {
        method, headers, body: data == null ? undefined : JSON.stringify(data), cache: 'no-store'
    });
    if (response.status === 401) {
        window.location.replace('/login');
        throw new Error('La sesión terminó.');
    }
    if (!response.ok) {
        const failure = await response.json().catch(() => null);
        throw new Error(failure?.message || (response.status === 403
            ? 'Tu cuenta no tiene acceso o la sesión venció. Actualiza la página.'
            : 'Ocurrió un error al comunicarse con el servidor.'));
    }
    return response.headers.get('Content-Type')?.includes('application/json') ? response.json() : null;
}

function toast(message) {
    clearTimeout(toastTimer);
    notification.textContent = message;
    notification.hidden = false;
    toastTimer = setTimeout(() => { notification.hidden = true; }, 5000);
}

function errorNotice(error) {
    return node('p', { class: 'notice error', role: 'alert' }, error.message);
}

function page(title, subtitle, actions = [], ...sections) {
    return [
        node('header', { class: 'page-head' },
            node('div', {}, node('h1', {}, title), subtitle ? node('p', { class: 'subtitle' }, subtitle) : null),
            node('div', { class: 'actions' }, actions)),
        ...sections
    ];
}

function panel(title, ...children) {
    return node('section', { class: 'panel' }, node('h2', {}, title), children);
}

function table(headers, rows, empty = 'Todavía no hay registros.') {
    if (!rows.length) return node('p', { class: 'notice' }, empty);
    return node('div', { class: 'table-wrap', tabindex: '0', role: 'region', 'aria-label': 'Tabla de registros' },
        node('table', {},
            node('thead', {}, node('tr', {}, headers.map(title => node('th', { scope: 'col' }, title)))),
            node('tbody', {}, rows.map(cells => node('tr', {}, cells.map(cell =>
                node('td', { class: cell?.classList?.contains('actions') ? 'actions-cell' : null }, cell ?? '—')))))
        ));
}

function actions(...items) {
    return node('div', { class: 'actions' }, items);
}

function bindForm(form, errorBox, handler) {
    form.addEventListener('submit', async event => {
        event.preventDefault();
        const submit = form.querySelector('button[type=submit]');
        if (submit.disabled) return;
        submit.disabled = true;
        errorBox.replaceChildren();
        try {
            await handler(form);
        } catch (error) {
            errorBox.replaceChildren(errorNotice(error));
        } finally {
            submit.disabled = false;
        }
    });
}

function submitButton(label) {
    return node('button', { class: 'primary', type: 'submit' }, label);
}

function editor(title, fields, handler, description) {
    const errors = node('div');
    const cancel = button('Cancelar', () => dialog.close());
    const form = node('form', { class: 'stack' },
        description ? node('p', { class: 'subtitle' }, description) : null,
        node('div', { class: 'form-grid' }, fields), errors, actions(cancel, submitButton('Guardar')));
    dialog.replaceChildren(node('h2', { id: 'dialog-title' }, title), form);
    bindForm(form, errors, async current => {
        await handler(current);
        dialog.close();
        toast('Cambios guardados.');
        await render();
    });
    dialog.showModal();
    return form;
}

function editRecord(title, definitions, path, row, transform = values) {
    editor(title, definitions, form => api(path + (row ? '/' + row.id : ''), row ? 'PUT' : 'POST', transform(form)));
}

function deleteButton(path, label, warning) {
    return button(label, async event => {
        const control = event.currentTarget;
        if (!window.confirm(warning)) return;
        control.disabled = true;
        try {
            await api(path, 'DELETE');
            toast('Registro eliminado.');
            await render();
        } catch (error) {
            toast(error.message);
        } finally {
            control.disabled = false;
        }
    }, 'danger');
}

function rowActions(onEdit, path, label = 'Eliminar', warning = '¿Eliminar el registro seleccionado?') {
    return actions(button('Editar', onEdit), path ? deleteButton(path, label, warning) : null);
}

async function dashboard() {
    const data = await api('/summary');
    state.today = data.date;
    const stat = (title, value, detail) => node('article', { class: 'stat' }, node('h2', {}, title),
        node('p', { class: 'value' }, value), detail ? node('p', { class: 'subtitle' }, detail) : null);
    return page('Inicio', 'Resumen del ' + displayDate(data.date), [],
        node('div', { class: 'stats' },
            stat('Cursos', numberFormat.format(data.courses)),
            roleIs('STUDENT') ? null : stat('Estudiantes', numberFormat.format(data.students)),
            stat('Profesores', numberFormat.format(data.teachers)),
            stat('Asistencia de hoy', data.attendancePercent == null ? '—' : data.attendancePercent + ' %',
                data.marked ? numberFormat.format(data.present) + ' presentes de ' + numberFormat.format(data.marked)
                    : 'Sin registros para hoy')));
}

async function people(path, teacher) {
    const rows = await api(path);
    const title = teacher ? 'Profesores' : 'Estudiantes';
    const person = teacher ? 'profesor' : 'estudiante';
    const edit = row => editRecord((row ? 'Editar ' : 'Nuevo ') + person, [
        field('document', 'Documento', { maxlength: 40, value: row?.document }),
        field('firstName', 'Nombres', { maxlength: 100, value: row?.firstName }),
        field('lastName', 'Apellidos', { maxlength: 100, value: row?.lastName }),
        ...(teacher ? [
            field('specialty', 'Especialidad', { maxlength: 120, value: row?.specialty }),
            field('email', 'Correo', { type: 'email', maxlength: 254, value: row?.email })
        ] : [
            field('gender', 'Género', { maxlength: 30, value: row?.gender }),
            field('section', 'Sección', { maxlength: 40, value: row?.section })
        ])
    ], path, row);
    const columns = teacher ? ['Documento', 'Profesor', 'Especialidad', 'Correo', 'Acciones']
        : ['Documento', 'Estudiante', 'Sección', 'Género', 'Acciones'];
    return page(title, '', [button('Nuevo ' + person, () => edit(null), 'primary')],
        table(columns, rows.map(row => [
            row.document, fullName(row), teacher ? row.specialty : row.section, teacher ? row.email : row.gender,
            rowActions(() => edit(row), path + '/' + row.id)
        ])));
}

async function coursesView() {
    const admin = roleIs('ADMIN');
    const [courses, teachers] = await Promise.all([api('/courses'), admin ? api('/teachers') : []]);
    const edit = row => {
        if (!teachers.length) {
            toast('Registra primero un profesor.');
            return;
        }
        editRecord(row ? 'Editar curso' : 'Nuevo curso', [
            field('code', 'Código', { maxlength: 40, value: row?.code }),
            field('name', 'Nombre', { maxlength: 160, value: row?.name }),
            field('teacherId', 'Profesor', {
                options: teachers.map(teacher => [teacher.id, fullName(teacher)]),
                placeholder: 'Selecciona un profesor', value: row?.teacherId
            }),
            field('section', 'Sección', { maxlength: 40, value: row?.section }),
            field('capacity', 'Capacidad', { type: 'number', min: 1, max: 5000, step: 1, value: row?.capacity })
        ], '/courses', row, form => ({
            ...values(form), teacherId: Number(fieldValue(form, 'teacherId')), capacity: Number(fieldValue(form, 'capacity'))
        }));
    };
    return page('Cursos', '', admin ? [button('Nuevo curso', () => edit(null), 'primary')] : [],
        table(['Código', 'Curso', 'Profesor', 'Sección', 'Ocupación', ...(admin ? ['Acciones'] : [])],
            courses.map(row => [row.code, row.name, row.teacherName, row.section, row.enrolled + ' / ' + row.capacity,
                ...(admin ? [rowActions(() => edit(row), '/courses/' + row.id)] : [])])));
}

async function enrollmentsView() {
    const admin = roleIs('ADMIN');
    const [courses, students] = await Promise.all([api('/courses'), admin ? api('/students') : []]);
    const selected = state.enrollmentCourse;
    const result = node('div');
    const select = selectCourse(courses, selected);
    const refresh = async courseId => {
        state.enrollmentCourse = courseId;
        if (!courseId) {
            result.replaceChildren(node('p', { class: 'notice' }, 'Selecciona un curso para consultar su matrícula.'));
            return;
        }
        const path = '/courses/' + courseId + '/enrollments';
        const rows = await api(path);
        if (courseId !== state.enrollmentCourse) return;
        const already = new Set(rows.map(row => row.studentId));
        const choices = students.filter(student => !already.has(student.id));
        const change = async (id, active) => {
            if (!active && !window.confirm('¿Retirar al estudiante de la matrícula activa? Conserva sus notas y asistencias.')) return;
            await api(path + '/' + id, 'PUT', { active });
            toast(active ? 'Matrícula activa.' : 'Matrícula retirada.');
            await refresh(courseId);
        };
        const form = node('form', { class: 'filters' });
        const errors = node('div');
        if (admin && choices.length) {
            form.append(field('studentId', 'Agregar estudiante', {
                options: choices.map(student => [student.id, student.document + ' · ' + fullName(student)]),
                placeholder: 'Selecciona un estudiante'
            }), submitButton('Matricular'));
            bindForm(form, errors, current => change(fieldValue(current, 'studentId'), true));
        }
        result.replaceChildren(form, errors, table(['Documento', 'Estudiante', 'Sección', 'Matrícula',
            ...(admin ? ['Acciones'] : [])], rows.map(row => [
            row.document, row.studentName, row.section, row.active ? 'Activa' : 'Retirada',
            ...(admin ? [actions(button(row.active ? 'Retirar' : 'Reactivar',
                event => invoke(event, () => change(row.studentId, !row.active)), row.active ? 'danger' : 'secondary'))] : [])
        ]), 'El curso aún no tiene estudiantes matriculados.'));
    };
    select.querySelector('select').addEventListener('change', event => {
        refresh(event.target.value).catch(error => result.replaceChildren(errorNotice(error)));
    });
    await refresh(select.querySelector('select').value);
    return page('Matrículas', '', [], panel('Curso', select), result);
}

async function invoke(event, effect) {
    const control = event.currentTarget;
    control.disabled = true;
    try {
        await effect();
    } catch (error) {
        toast(error.message);
    } finally {
        control.disabled = false;
    }
}

async function attendanceView() {
    const courses = await api('/courses');
    const result = node('div');
    const errors = node('div');
    const form = node('form', { class: 'filters' },
        selectCourse(courses, state.attendanceCourse),
        field('date', 'Fecha', { type: 'date', value: state.attendanceDate || state.today }),
        submitButton('Cargar lista'));
    const load = async current => {
        const courseId = Number(fieldValue(current, 'courseId'));
        const date = fieldValue(current, 'date');
        const roster = await api('/attendance/roster' + query({ courseId, date }));
        state.attendanceCourse = courseId;
        state.attendanceDate = date;
        if (!roster.length) {
            result.replaceChildren(node('p', { class: 'notice' }, 'El curso no tiene estudiantes para esta fecha.'));
            return;
        }
        const message = node('div');
        const markForm = node('form', { class: 'stack' },
            table(['Estudiante', 'Estado'], roster.map(row => [
                row.studentName,
                node('select', { name: 'mark-' + row.studentId, 'aria-label': 'Estado de ' + row.studentName, required: true },
                    node('option', { value: '' }, 'Selecciona un estado'),
                    optionList(statuses).map(([code, label]) => node('option', { value: code, selected: row.status === code }, label)))
            ])),
            message, actions(button('Marcar a todos presentes', () => {
                for (const select of markForm.querySelectorAll('select')) select.value = 'PRESENTE';
            }), submitButton('Guardar asistencia')));
        bindForm(markForm, message, async marks => {
            await api('/attendance', 'PUT', {
                courseId, date, marks: roster.map(row => ({
                    studentId: row.studentId, status: fieldValue(marks, 'mark-' + row.studentId)
                }))
            });
            state.attendanceFilters = { courseId, from: date, to: date };
            toast('Asistencia guardada.');
            window.location.hash = 'historial';
        });
        result.replaceChildren(panel('Lista del ' + displayDate(date), markForm));
    };
    form.addEventListener('change', () => result.replaceChildren());
    bindForm(form, errors, load);
    if (fieldValue(form, 'courseId') && fieldValue(form, 'date')) await load(form);
    return page('Pasar lista', '', [], form, errors, result);
}

async function historyView(grades) {
    const courses = await api('/courses');
    const key = grades ? 'gradeFilters' : 'attendanceFilters';
    const filters = state[key] || {};
    const path = grades ? '/grades' : '/attendance';
    const errors = node('div');
    const results = node('div');
    const form = node('form', { class: 'filters' },
        selectCourse(courses, filters.courseId, true),
        field('from', 'Desde', { type: 'date', required: false, value: filters.from }),
        field('to', 'Hasta', { type: 'date', required: false, value: filters.to }),
        submitButton('Aplicar filtros'));
    const download = node('a', { class: 'button secondary' }, 'Descargar CSV');
    const load = async current => {
        const criteria = values(current);
        const suffix = query(criteria);
        const rows = await api(path + suffix);
        state[key] = criteria;
        download.href = '/api' + path + '.csv' + suffix;
        const staff = canTeach();
        if (grades) {
            results.replaceChildren(table(['Fecha', 'Curso', 'Evaluación', ...(staff ? ['Estudiante'] : []), 'Nota'],
                rows.map(row => [displayDate(row.date), row.courseName, row.assessment,
                    ...(staff ? [row.studentName] : []), numberFormat.format(row.score)])));
        } else {
            results.replaceChildren(table(['Fecha', 'Curso', ...(staff ? ['Estudiante'] : []), 'Estado',
                ...(staff ? ['Acciones'] : [])], rows.map(row => [
                displayDate(row.date), row.courseCode + ' · ' + row.courseName,
                ...(staff ? [row.studentName] : []), statuses[row.status],
                ...(staff ? [actions(deleteButton('/attendance' + query({
                    courseId: row.courseId, date: row.date, studentId: row.studentId
                }), 'Eliminar', '¿Eliminar la marca de asistencia del estudiante para esta fecha?'))] : [])
            ])));
        }
    };
    bindForm(form, errors, load);
    await load(form);
    return page(grades ? 'Calificaciones' : 'Historial de asistencia', '', [download], form, errors, results);
}

async function scheduleView() {
    const admin = roleIs('ADMIN');
    const [rows, courses] = await Promise.all([api('/schedule'), admin ? api('/courses') : []]);
    const edit = row => {
        if (!courses.length) {
            toast('Crea primero un curso.');
            return;
        }
        editRecord(row ? 'Editar horario' : 'Nuevo horario', [
            selectCourse(courses, row?.courseId),
            field('weekday', 'Día', { options: weekdays.map((day, i) => [i + 1, day]), value: row?.weekday }),
            field('startsAt', 'Hora de inicio', { type: 'time', value: row?.startsAt.slice(0, 5) }),
            field('endsAt', 'Hora de fin', { type: 'time', value: row?.endsAt.slice(0, 5) }),
            field('room', 'Aula', { maxlength: 100, value: row?.room })
        ], '/schedule', row, form => ({
            ...values(form), courseId: Number(fieldValue(form, 'courseId')), weekday: Number(fieldValue(form, 'weekday'))
        }));
    };
    return page('Horarios', '', admin ? [button('Nuevo horario', () => edit(null), 'primary')] : [],
        table(['Día', 'Horario', 'Curso', 'Aula', ...(admin ? ['Acciones'] : [])], rows.map(row => [
            weekdays[row.weekday - 1], row.startsAt.slice(0, 5) + '–' + row.endsAt.slice(0, 5), row.courseName,
            row.room, ...(admin ? [rowActions(() => edit(row), '/schedule/' + row.id)] : [])
        ])));
}

async function assessmentsView() {
    const [rows, courses] = await Promise.all([api('/assessments'), api('/courses')]);
    const selected = state.assessmentId;
    const marks = node('div');
    const edit = row => {
        if (!courses.length) {
            toast('Se necesita un curso para crear una evaluación.');
            return;
        }
        editRecord(row ? 'Editar evaluación' : 'Nueva evaluación', [
            row ? null : selectCourse(courses),
            field('title', 'Título', { maxlength: 160, value: row?.title }),
            field('date', 'Fecha', { type: 'date', value: row?.date || state.today })
        ], '/assessments', row, form => ({
            ...values(form), courseId: row?.courseId || Number(fieldValue(form, 'courseId'))
        }));
    };
    const load = async row => {
        state.assessmentId = row.id;
        const path = '/assessments/' + row.id + '/grades';
        const roster = await api(path);
        if (state.assessmentId !== row.id) return;
        if (!roster.length) {
            marks.replaceChildren(panel(row.title, node('p', { class: 'notice' }, 'El curso aún no tiene estudiantes matriculados.')));
            return;
        }
        const errors = node('div');
        const form = node('form', { class: 'stack' },
            node('p', { class: 'subtitle' }, 'Escala de 0 a 20. Deja el campo vacío para mantenerlo sin nota.'),
            table(['Estudiante', 'Nota'], roster.map(person => [
                person.studentName,
                node('input', { name: 'grade-' + person.studentId, type: 'number', min: 0, max: 20, step: '0.01',
                    value: person.score, 'aria-label': 'Nota de ' + person.studentName })
            ])), errors, actions(submitButton('Guardar calificaciones')));
        bindForm(form, errors, async current => {
            await api(path, 'PUT', { marks: roster.map(person => {
                const score = optional(current, 'grade-' + person.studentId);
                return { studentId: person.studentId, score: score == null ? null : Number(score) };
            }) });
            toast('Calificaciones guardadas.');
        });
        marks.replaceChildren(panel(row.title, form));
    };
    const selectedRow = rows.find(row => row.id === selected);
    if (selectedRow) await load(selectedRow);
    return page('Evaluaciones', '', [button('Nueva evaluación', () => edit(null), 'primary')],
        panel('Evaluaciones por curso', table(['Fecha', 'Curso', 'Evaluación', 'Acciones'], rows.map(row => [
            displayDate(row.date), row.courseName, row.title,
            actions(button('Calificar', event => invoke(event, () => load(row))),
                button('Editar', () => edit(row)), deleteButton('/assessments/' + row.id, 'Eliminar',
                    '¿Eliminar la evaluación y las notas registradas?'))
        ]))), marks);
}

async function accountsView() {
    const [rows, teachers, students] = await Promise.all([api('/accounts'), api('/teachers'), api('/students')]);
    const edit = row => {
        const teacher = field('teacherId', 'Profesor vinculado', {
            options: teachers.map(item => [item.id, item.document + ' · ' + fullName(item)]),
            placeholder: 'Selecciona un profesor', value: row?.teacherId
        });
        const student = field('studentId', 'Estudiante vinculado', {
            options: students.map(item => [item.id, item.document + ' · ' + fullName(item)]),
            placeholder: 'Selecciona un estudiante', value: row?.studentId
        });
        const form = editor(row ? 'Editar usuario' : 'Nuevo usuario', [
            field('name', 'Nombre', { maxlength: 160, value: row?.name }),
            field('email', 'Correo', { type: 'email', maxlength: 254, value: row?.email }),
            field('role', 'Rol', { options: optionList(roles), value: row?.role || 'STUDENT' }),
            field('password', row ? 'Nueva contraseña (opcional)' : 'Contraseña inicial',
                { type: 'password', autocomplete: 'new-password', minlength: 12, maxlength: 72, required: !row }),
            field('enabled', 'Estado', { options: [['true', 'Activa'], ['false', 'Desactivada']],
                value: String(row?.enabled ?? true) }),
            teacher, student
        ], current => {
            const role = fieldValue(current, 'role');
            return api('/accounts' + (row ? '/' + row.id : ''), row ? 'PUT' : 'POST', {
                ...values(current), enabled: fieldValue(current, 'enabled') === 'true',
                teacherId: role === 'TEACHER' ? Number(fieldValue(current, 'teacherId')) : null,
                studentId: role === 'STUDENT' ? Number(fieldValue(current, 'studentId')) : null
            });
        });
        const toggleProfile = () => {
            const role = fieldValue(form, 'role');
            for (const [group, expected] of [[teacher, 'TEACHER'], [student, 'STUDENT']]) {
                group.hidden = role !== expected;
                group.querySelector('select').disabled = group.hidden;
            }
        };
        form.elements.namedItem('role').addEventListener('change', toggleProfile);
        toggleProfile();
    };
    return page('Usuarios', '', [button('Nuevo usuario', () => edit(null), 'primary')],
        table(['Nombre', 'Correo', 'Rol', 'Estado', 'Acciones'], rows.map(row => [
            row.name, row.email, roles[row.role], row.enabled ? 'Activa' : 'Desactivada',
            row.id === state.account.id ? node('a', { href: '#cuenta' }, 'Mi cuenta')
                : rowActions(() => edit(row), null)
        ])));
}

function profileView() {
    const { name, email, role } = state.account;
    const errors = node('div');
    const form = node('form', { class: 'stack' },
        field('currentPassword', 'Contraseña actual', { type: 'password', autocomplete: 'current-password', maxlength: 72 }),
        field('newPassword', 'Nueva contraseña', { type: 'password', autocomplete: 'new-password', minlength: 12, maxlength: 72 }),
        errors, actions(submitButton('Cambiar contraseña')));
    bindForm(form, errors, async current => {
        await api('/account/password', 'PUT', values(current));
        window.location.replace('/login?clave');
    });
    return page('Mi cuenta', '', [], panel('Perfil', node('p', {}, name),
        node('p', { class: 'subtitle' }, email + ' · ' + roles[role])), panel('Cambiar contraseña', form));
}

async function legacyView() {
    const rows = await api('/legacy');
    const errors = node('div');
    const uploads = [['students', 'Estudiantes.txt'], ['teachers', 'Profesor.txt'], ['courses', 'Cursos.txt'],
        ['attendance', 'Asistencias.txt']];
    const form = node('form', { class: 'stack' },
        node('div', { class: 'form-grid' }, uploads.map(([key, label]) =>
            field(key, label, { type: 'file', accept: '.txt,.csv,text/plain', required: false }))),
        errors, actions(submitButton('Importar archivos')));
    bindForm(form, errors, async current => {
        const files = {};
        for (const [key] of uploads) {
            const file = current.elements.namedItem(key).files[0];
            if (file?.size > 500000) throw new Error('Cada archivo debe pesar como máximo 500 KB.');
            if (file) files[key] = await file.text();
        }
        if (Object.keys(files).length === 0) throw new Error('Selecciona al menos un archivo.');
        const result = await api('/legacy', 'POST', files);
        toast('Importados: ' + result.students + ' estudiantes, ' + result.teachers + ' profesores, '
            + result.courses + ' cursos y ' + result.attendance + ' asistencias.');
        await render();
    });
    return page('Importación', '', [], panel('Archivos de la aplicación de escritorio',
        node('p', { class: 'subtitle' }, 'Usa archivos de texto UTF-8. El archivo de usuarios requiere crear cuentas nuevas.'),
        form), panel('Asistencias históricas sin curso',
        actions(node('a', { href: '/api/legacy/attendance.csv', class: 'button secondary' }, 'Descargar CSV')),
        table(['Fecha', 'Estudiante', 'Estado'], rows.map(row => [displayDate(row.date), row.studentName, statuses[row.status]]))));
}

function routes() {
    const admin = roleIs('ADMIN');
    return {
        inicio: ['Inicio', dashboard],
        ...(admin ? { estudiantes: ['Estudiantes', () => people('/students', false)],
            profesores: ['Profesores', () => people('/teachers', true)] } : {}),
        cursos: ['Cursos', coursesView],
        ...(canTeach() ? { matriculas: ['Matrículas', enrollmentsView], asistencia: ['Pasar lista', attendanceView] } : {}),
        historial: ['Historial de asistencia', () => historyView(false)],
        horarios: ['Horarios', scheduleView],
        ...(canTeach() ? { evaluaciones: ['Evaluaciones', assessmentsView] } : {}),
        notas: ['Calificaciones', () => historyView(true)],
        ...(admin ? { usuarios: ['Usuarios', accountsView], importar: ['Importación', legacyView] } : {}),
        cuenta: ['Mi cuenta', profileView]
    };
}

async function render(focus = false) {
    const pages = routes();
    const requested = window.location.hash.slice(1);
    if (requested === 'contenido') {
        content.focus();
        return;
    }
    const route = Object.hasOwn(pages, requested) ? requested : 'inicio';
    const version = ++state.viewVersion;
    const [title, load] = pages[route];
    const currentLinks = Object.entries(pages).filter(([name]) => name !== 'cuenta');
    navigation.replaceChildren(...currentLinks.map(([name, [label]]) =>
        node('a', { href: '#' + name, 'aria-current': route === name ? 'page' : null }, label)));
    document.title = title + ' | Gestión Académica';
    content.setAttribute('aria-busy', 'true');
    content.replaceChildren(node('p', { class: 'notice', role: 'status' }, 'Cargando…'));
    try {
        const result = await load();
        if (version !== state.viewVersion) return;
        content.replaceChildren(...result);
    } catch (error) {
        if (version !== state.viewVersion) return;
        content.replaceChildren(...page(title, '', [], errorNotice(error), button('Volver a cargar', () => render())));
    }
    content.setAttribute('aria-busy', 'false');
    if (focus) content.focus();
}

function setMenu(open) {
    sidebar.classList.toggle('open', open);
    menuToggle.setAttribute('aria-expanded', String(open));
}

async function start() {
    try {
        Object.assign(state, await api('/me'));
        const data = await api('/summary');
        state.today = data.date;
        document.querySelector('#identity').textContent = state.account.name + ' · ' + roles[state.account.role];
        menuToggle.addEventListener('click', () => setMenu(!sidebar.classList.contains('open')));
        window.addEventListener('hashchange', () => {
            setMenu(false);
            render(true);
        });
        if (window.location.hash === '#contenido') history.replaceState(null, '', '#inicio');
        await render();
    } catch (error) {
        content.setAttribute('aria-busy', 'false');
        content.replaceChildren(errorNotice(error), button('Volver a cargar', () => window.location.reload()));
    }
}

start();
