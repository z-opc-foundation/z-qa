import assert from 'node:assert/strict';
import {readFileSync} from 'node:fs';
import test from 'node:test';
import vm from 'node:vm';
import ts from 'typescript';

// No test/DOM dependencies are installed. Execute the actual TSX with a minimal
// hook/UI boundary harness; all API calls are deferred mocks, never HTTP.
// This covers component state/handlers, not Ant Design's DOM or browser layout.
const source = ts.transpileModule(readFileSync(new URL('./SuiteSteps.tsx', import.meta.url), 'utf8'), {
    compilerOptions: {module: ts.ModuleKind.CommonJS, jsx: ts.JsxEmit.React, target: ts.ScriptTarget.ES2020, esModuleInterop: true},
}).outputText;
const ok = content => ({success: true, code: 200, content});
const fail = {success: false, code: 500, msg: 'mock business failure'};
const fixtures = (count = 2) => Array.from({length: count}, (_, i) => ({
    step_no: i + 1, step_name: `step-${i + 1}`, method: 'GET', url: `/mock/${i + 1}`,
    execution_mode: 'api', headers_json: '{"existing":true}', backend_field: 'preserve me',
}));
const deferred = () => {
    let resolve, reject;
    const promise = new Promise((yes, no) => { resolve = yes; reject = no; });
    return {promise, resolve, reject};
};
const copy = value => JSON.parse(JSON.stringify(value));

function nodes(tree, type) {
    if (!tree || typeof tree !== 'object') return [];
    const own = tree.type === type ? [tree] : [];
    return own.concat(Object.values(tree.props || tree).flatMap(value => nodes(value, type)));
}
function button(tree, label) {
    const result = nodes(tree, 'Button').find(node => node.props.children === label);
    assert.ok(result, `button ${label} exists`);
    return result.props;
}

function harness(initialId = '1') {
    let instance, slot = 0, changed = true, tree, idProp = initialId, routeId;
    const loads = [], saves = [], notices = [];
    const forms = [];
    const formFactory = () => ({
        values: {}, validation: null,
        resetFields() { this.values = {}; },
        setFieldsValue(values) { Object.assign(this.values, values); },
        validateFields() { return this.validation?.promise || Promise.resolve({...this.values}); },
    });
    function hook(init) {
        const index = slot++;
        if (!(index in instance.hooks)) instance.hooks[index] = init();
        return instance.hooks[index];
    }
    const React = {
        createElement: (type, props, ...children) => ({type, props: {...props, children: children.length === 1 ? children[0] : children}}),
        useState(initial) {
            const owner = instance;
            const state = hook(() => ({value: initial}));
            return [state.value, value => {
                assert.equal(owner.disposed, false, 'no state update after unmount');
                state.value = typeof value === 'function' ? value(state.value) : value;
                changed = true;
            }];
        },
        useRef: initial => hook(() => ({current: initial})),
        useEffect(effect, deps) {
            const state = hook(() => ({deps: undefined, cleanup: undefined}));
            if (!state.deps || deps.some((dep, index) => dep !== state.deps[index])) {
                instance.effects.push(() => {
                    state.cleanup?.();
                    state.deps = deps;
                    state.cleanup = effect();
                });
            }
        },
    };
    const Form = Object.assign(function Form() {}, {
        Item: 'Form.Item', useForm: () => [hook(() => {
            const form = formFactory();
            forms.push(form);
            return form;
        })],
    });
    const antd = Object.fromEntries(['Alert', 'Button', 'Card', 'InputNumber', 'Modal', 'Popconfirm', 'Space', 'Table', 'Tag', 'Tooltip'].map(name => [name, name]));
    Object.assign(antd, {
        Form, Input: Object.assign(function Input() {}, {TextArea: 'Input.TextArea'}),
        Select: Object.assign(function Select() {}, {Option: 'Select.Option'}),
        message: {success: text => notices.push(['success', text]), error: text => notices.push(['error', text])},
    });
    const api = {
        steps(id) { const request = {...deferred(), id}; loads.push(request); return request.promise; },
        saveSteps(id, steps) { const request = {...deferred(), id, steps: copy(steps)}; saves.push(request); return request.promise; },
    };
    const modules = {
        react: React, antd, '@ant-design/icons': {PlusOutlined: 'PlusOutlined', SaveOutlined: 'SaveOutlined'},
        '../services/api': {suiteApi: api}, '../qaBase': {useQaBase: () => '/z-qa'},
        'react-router-dom': {useParams: () => ({id: routeId}), useLocation: () => ({state: {suite: {id: 1, name: 'first suite'}}}), useNavigate: () => () => {}},
    };
    const exports = {};
    vm.runInNewContext(source, {exports, require: name => {
        assert.ok(name in modules, `unexpected import ${name}`);
        return modules[name];
    }});
    function unmount() {
        instance?.hooks.forEach(state => state.cleanup?.());
        if (instance) instance.disposed = true;
    }
    function render() {
        const element = exports.default({id: idProp});
        if (!instance || element.props.key !== instance.key) {
            unmount();
            instance = {key: element.props.key, hooks: [], effects: [], disposed: false};
        }
        changed = false;
        slot = 0;
        tree = element.type(element.props);
        instance.effects.splice(0).forEach(effect => effect());
    }
    async function flush() {
        for (let i = 0; i < 10; i++) {
            await Promise.resolve();
            if (changed) render();
        }
    }
    render();
    const result = {
        loads, saves, notices, flush, unmount,
        get tree() { return tree; },
        get form() { return forms.at(-1); },
        get table() { return nodes(tree, 'Table')[0].props; },
        get rows() { return result.table.dataSource; },
        get modal() { return nodes(tree, 'Modal').find(node => node.props.title !== '确认清空全部步骤？').props; },
        get clear() { return nodes(tree, 'Modal').find(node => node.props.title === '确认清空全部步骤？').props; },
        top: label => button(tree, label),
        row(no, pageIndex = 0) {
            const row = result.rows.find(step => step.step_no === no);
            return result.table.columns.find(column => column.title === '操作').render(null, row, pageIndex);
        },
        async switchId(id, viaRoute = false) {
            idProp = viaRoute ? undefined : id;
            routeId = viaRoute ? id : undefined;
            render();
            await flush();
        },
        async loaded(steps = fixtures()) { loads.at(-1).resolve(ok(steps)); await flush(); },
        async add() { result.top('新增步骤').onClick(); await flush(); result.form.setFieldsValue({step_name: 'new step', url: '/mock/new'}); },
        async move(no, direction, pageIndex = 0) { button(result.row(no, pageIndex), direction).onClick(); await flush(); },
        async remove(no) { nodes(result.row(no), 'Popconfirm')[0].props.onConfirm(); await flush(); },
    };
    return result;
}

test('loading and failed loads block writes; retry unlocks only after success', async () => {
    const h = harness();
    assert.equal(h.table.loading, true);
    assert.equal(h.top('新增步骤').disabled, true);
    assert.equal(h.top('保存全部').disabled, true);
    h.top('新增步骤').onClick();
    await h.top('保存全部').onClick();
    await h.modal.onOk();
    assert.equal(h.saves.length, 0);
    h.loads[0].reject(new Error('offline'));
    await h.flush();
    assert.equal(h.table.loading, false);
    assert.equal(h.top('新增步骤').disabled, true);
    assert.match(nodes(h.tree, 'Alert')[0].props.title, /offline/);
    h.top('重试').onClick();
    await h.flush();
    assert.equal(h.loads.length, 2);
    assert.equal(h.table.loading, true);
    assert.equal(h.top('新增步骤').disabled, true);
    await h.loaded();
    assert.equal(h.top('新增步骤').disabled, false);
    assert.equal(h.top('保存全部').disabled, true);
    assert.equal(nodes(h.tree, 'Alert').length, 0, 'no old informational callout');
});

for (const [name, response] of [['business failure', fail], ['null content', ok(null)], ['object content', ok({})]]) {
    test(`${name} is not treated as an empty editable suite`, async () => {
        const h = harness();
        h.loads[0].resolve(response);
        await h.flush();
        assert.equal(h.top('新增步骤').disabled, true);
        assert.equal(h.top('保存全部').disabled, true);
        assert.equal(nodes(h.tree, 'Alert').length, 1);
        await h.top('保存全部').onClick();
        assert.equal(h.saves.length, 0);
    });
}

test('missing or invalid IDs do not load or save', async () => {
    for (const id of [undefined, '', '1bad', '-1']) {
        const h = harness(id === undefined ? '' : id);
        if (id === undefined) await h.switchId(undefined, true);
        await h.flush();
        assert.equal(h.loads.length, 0);
        assert.equal(h.top('新增步骤').disabled, true);
        await h.top('保存全部').onClick();
        assert.equal(h.saves.length, 0);
    }
});

test('prop/route ID changes discard drafts and ignore stale load success or failure', async () => {
    for (const rejects of [false, true]) {
        const h = harness();
        await h.switchId('2', true);
        await h.loaded(fixtures(3));
        if (rejects) h.loads[0].reject(new Error('stale failure'));
        else h.loads[0].resolve(ok(fixtures(20)));
        await h.flush();
        assert.equal(h.rows.length, 3);
        assert.equal(nodes(h.tree, 'Alert').length, 0);
        await h.move(1, '下移');
        await h.add();
        await h.switchId('3');
        assert.equal(h.rows.length, 0);
        assert.equal(h.modal.open, false);
        assert.equal(h.clear.open, false);
        assert.equal(h.top('新增步骤').disabled, true);
        assert.equal(h.top('保存全部').disabled, true);
        assert.deepEqual(h.form.values, {});
        assert.ok(nodes(nodes(h.tree, 'Card')[0].props.title, 'span').some(node => node.props.children === '套件步骤 — 3'));
    }
});

test('modal add persists once, holds list/dialog while pending, and closes only on success', async () => {
    const h = harness();
    await h.loaded();
    await h.add();
    const submit = h.modal.onOk;
    const first = submit();
    await submit();
    await h.flush();
    assert.equal(h.saves.length, 1);
    assert.equal(h.saves[0].id, 1);
    assert.equal(h.saves[0].steps.length, 3);
    assert.equal(h.saves[0].steps[2].execution_mode, 'api');
    assert.equal(h.rows.length, 2);
    assert.equal(h.modal.open, true);
    assert.equal(h.modal.confirmLoading, true);
    assert.equal(h.modal.cancelButtonProps.disabled, true);
    assert.equal(h.top('新增步骤').disabled, true);
    h.modal.onCancel();
    await h.modal.onOk();
    await h.flush();
    assert.equal(h.modal.open, true);
    assert.equal(h.saves.length, 1);
    h.saves[0].resolve(ok(null));
    await first;
    await h.flush();
    assert.equal(h.rows.length, 3);
    assert.equal(h.modal.open, false);
    assert.equal(h.top('保存全部').disabled, true, 'modal does not require a second save');
});

test('validation errors keep the form and release the pending lock', async () => {
    const h = harness();
    await h.loaded([]);
    await h.add();
    h.form.validation = deferred();
    const submit = h.modal.onOk();
    await h.modal.onOk();
    h.form.validation.reject({errorFields: [{name: 'url'}]});
    await submit;
    await h.flush();
    assert.equal(h.saves.length, 0);
    assert.equal(h.modal.open, true);
    assert.equal(h.modal.confirmLoading, false);
    assert.equal(h.form.values.step_name, 'new step');
    assert.equal(h.notices.length, 0);
});

for (const mode of ['network', 'business']) {
    test(`modal ${mode} failure preserves form/list and can be retried`, async () => {
        const h = harness();
        await h.loaded();
        button(h.row(1), '编辑').onClick();
        await h.flush();
        h.form.setFieldsValue({step_name: 'edited', body: 'keep input'});
        const save = h.modal.onOk();
        await h.flush();
        if (mode === 'network') h.saves[0].reject(new Error('offline'));
        else h.saves[0].resolve(fail);
        await save;
        await h.flush();
        assert.equal(h.rows[0].step_name, 'step-1');
        assert.equal(h.modal.open, true);
        assert.equal(h.modal.confirmLoading, false);
        assert.equal(h.form.values.body, 'keep input');
        assert.equal(h.notices.at(-1)[0], 'error');
        const retry = h.modal.onOk();
        await h.flush();
        assert.equal(h.saves[1].steps[0].backend_field, 'preserve me');
        h.saves[1].resolve(ok(null));
        await retry;
        await h.flush();
        assert.equal(h.rows[0].step_name, 'edited');
        assert.equal(h.modal.open, false);
    });
}

test('pagination-local row index cannot move the wrong global row; boundaries are disabled', async () => {
    const h = harness();
    const initial = fixtures(25).map(step => ({...step, step_no: step.step_no * 10}));
    await h.loaded(initial);
    assert.equal(button(h.row(10), '上移').disabled, true);
    assert.equal(button(h.row(250), '下移').disabled, true);
    await h.move(210, '上移', 0); // First visible row on page 2, global index 20.
    assert.equal(h.rows[0].step_name, 'step-1');
    assert.equal(h.rows[19].step_name, 'step-21');
    assert.equal(h.rows[20].step_name, 'step-20');
    assert.deepEqual(copy(h.rows.map(step => step.step_no)), Array.from({length: 25}, (_, i) => i + 1));
    assert.equal(h.saves.length, 0);
    assert.equal(h.top('保存全部').disabled, false);
    await h.move(1, '上移');
    await h.move(25, '下移');
    const save = h.top('保存全部').onClick();
    await h.top('保存全部').onClick();
    await h.flush();
    assert.equal(h.saves.length, 1);
    assert.equal(button(h.row(20), '编辑').disabled, true);
    assert.equal(button(h.row(20), '上移').disabled, true);
    const before = copy(h.rows);
    await h.move(20, '下移');
    await h.remove(1);
    assert.deepEqual(copy(h.rows), before);
    h.saves[0].resolve(ok(null));
    await save;
    await h.flush();
    assert.deepEqual(copy(h.rows), h.saves[0].steps);
    assert.equal(h.top('保存全部').disabled, true);
});

test('batch failure retains inline changes for retry; modal can also commit that draft', async () => {
    const h = harness();
    await h.loaded();
    await h.move(2, '上移');
    const save = h.top('保存全部').onClick();
    h.saves[0].resolve(fail);
    await save;
    await h.flush();
    assert.equal(h.top('保存全部').disabled, false);
    assert.equal(h.rows[0].step_name, 'step-2');
    await h.add();
    const modalSave = h.modal.onOk();
    await h.flush();
    assert.equal(h.saves[1].steps[0].step_name, 'step-2');
    h.saves[1].resolve(ok(null));
    await modalSave;
    await h.flush();
    assert.equal(h.top('保存全部').disabled, true);
});

test('delete needs confirmation; saving the final deletion needs explicit clear confirmation', async () => {
    const h = harness();
    await h.loaded(fixtures(1));
    const row = h.row(1);
    assert.equal(button(row, '删除').onClick, undefined, 'delete button cannot bypass Popconfirm');
    assert.equal(nodes(row, 'Popconfirm').length, 1);
    assert.equal(h.rows.length, 1, 'opening/cancelling confirmation does not remove');
    await h.remove(1);
    assert.equal(h.rows.length, 0);
    assert.equal(h.saves.length, 0, 'inline delete is local');
    await h.top('保存全部').onClick();
    await h.flush();
    assert.equal(h.clear.open, true);
    assert.equal(h.saves.length, 0);
    h.clear.onCancel();
    await h.flush();
    assert.equal(h.clear.open, false);
    assert.equal(h.top('保存全部').disabled, false);
    await h.top('保存全部').onClick();
    await h.flush();
    const clearing = h.clear.onOk();
    await h.clear.onOk();
    await h.flush();
    assert.equal(h.saves.length, 1);
    assert.deepEqual(h.saves[0].steps, []);
    assert.equal(h.clear.cancelButtonProps.disabled, true);
    h.saves[0].resolve(fail);
    await clearing;
    await h.flush();
    assert.equal(h.clear.open, true, 'failed clear remains retryable');
    const retry = h.clear.onOk();
    h.saves[1].resolve(ok(null));
    await retry;
    await h.flush();
    assert.equal(h.clear.open, false);
    assert.equal(h.top('保存全部').disabled, true);
});

test('an initially empty suite is not dirty and cannot issue a clearing request', async () => {
    const h = harness();
    await h.loaded([]);
    await h.top('保存全部').onClick();
    await h.clear.onOk();
    assert.equal(h.top('保存全部').disabled, true);
    assert.equal(h.saves.length, 0);
    assert.equal(h.clear.open, false);
});

test('switching ID during validation does not send the obsolete form', async () => {
    const h = harness();
    await h.loaded();
    await h.add();
    const validation = deferred();
    h.form.validation = validation;
    const submit = h.modal.onOk();
    await h.switchId('2');
    validation.resolve({step_name: 'obsolete', url: '/mock'});
    await submit;
    await h.flush();
    assert.equal(h.saves.length, 0);
    assert.equal(h.modal.open, false);
});

test('stale save responses cannot alter the new suite, close its modal, or show notices', async () => {
    for (const rejects of [false, true]) {
        const h = harness();
        await h.loaded();
        await h.add();
        const submit = h.modal.onOk();
        await h.flush();
        await h.switchId('2');
        await h.loaded(fixtures(5));
        await h.add();
        if (rejects) h.saves[0].reject(new Error('stale failure'));
        else h.saves[0].resolve(ok(null));
        await submit;
        await h.flush();
        assert.equal(h.rows.length, 5);
        assert.equal(h.modal.open, true);
        assert.equal(h.form.values.step_name, 'new step');
        assert.equal(h.notices.length, 0);
    }
});

test('old delete/clear callbacks are inert after an ID switch or unmount', async () => {
    const h = harness();
    await h.loaded(fixtures(1));
    const remove = nodes(h.row(1), 'Popconfirm')[0].props.onConfirm;
    await h.remove(1);
    await h.top('保存全部').onClick();
    await h.flush();
    const clear = h.clear.onOk;
    await h.switchId('2');
    await h.loaded(fixtures(3));
    remove();
    await clear();
    await h.flush();
    assert.equal(h.rows.length, 3);
    assert.equal(h.saves.length, 0);
    h.unmount();
    await h.modal.onOk();
    assert.equal(h.saves.length, 0);
});
