import React, {useEffect, useRef, useState} from 'react';
import {Alert, Button, Card, Form, Input, InputNumber, message, Modal, Popconfirm, Select, Space, Table, Tag, Tooltip} from 'antd';
import {PlusOutlined, SaveOutlined} from '@ant-design/icons';
import {suiteApi} from '../services/api';
import {useLocation, useNavigate, useParams} from 'react-router-dom';
import {useQaBase} from '../qaBase';

/**
 * 套件步骤编辑器 - 链路可视化雏形 (步骤列表, 后续可改 LogicFlow).
 * 路径: /z-qa/suites/:id/steps
 */
const SuiteSteps: React.FC<{ id?: string }> = ({id: idProp}) => {
    // 宿主可能不用 <Route :id> 分发（Z-Team 工作台按 pathname 直接渲染），id 由宿主传入。
    const {id: routeId} = useParams<{ id: string }>();
    const id = idProp ?? routeId;
    // id 切换时卸载旧编辑器，表单、草稿、确认框和请求状态不能串到新套件。
    return <SuiteStepsEditor key={id} id={id}/>;
};

const SuiteStepsEditor: React.FC<{ id?: string }> = ({id}) => {
    const location = useLocation();
    const navigate = useNavigate();
    const qaRoot = useQaBase();
    const suite = (location.state as any)?.suite;
    const [steps, setSteps] = useState<any[]>([]);
    const [editing, setEditing] = useState<any>(null);
    const [modalOpen, setModalOpen] = useState(false);
    const [form] = Form.useForm();
    const [loadState, setLoadState] = useState<'loading' | 'ready' | 'error'>('loading');
    const [loadError, setLoadError] = useState('');
    const [retry, setRetry] = useState(0);
    const [dirty, setDirty] = useState(false);
    const [saving, setSaving] = useState(false);
    const [clearOpen, setClearOpen] = useState(false);
    const active = useRef(false);
    // 同一轮渲染中的重复点击也必须被拦截（包含表单校验期间）。
    const savingRef = useRef(false);
    const canWrite = () => active.current && loadState === 'ready' && !savingRef.current;
    const inlineDisabled = loadState !== 'ready' || saving || modalOpen || clearOpen;

    useEffect(() => {
        let current = true;
        active.current = true;
        setLoadState('loading');
        setLoadError('');
        setSteps([]);
        const load = async () => {
            try {
                if (!id || !Number.isSafeInteger(Number(id)) || Number(id) <= 0) {
                    throw new Error('无效的套件 ID');
                }
                const res = await suiteApi.steps(Number(id));
                // 业务失败或非数组响应不能当作空套件，否则全量保存可能误清空。
                if (res?.success !== true || !Array.isArray(res.content)) {
                    throw new Error(res?.msg || '步骤响应无效');
                }
                if (!current) return;
                setSteps(res.content);
                setLoadState('ready');
            } catch (e: any) {
                if (!current) return;
                setLoadError(e?.message || '加载失败');
                setLoadState('error');
            }
        };
        void load();
        return () => {
            current = false;
            active.current = false;
        };
    }, [id, retry]);

    const handleAdd = () => {
        if (!canWrite() || modalOpen || clearOpen) return;
        setEditing(null);
        form.resetFields();
        form.setFieldsValue({
            execution_mode: 'api',
            method: 'GET',
            target_mode: 'real',
            timeout_ms: 10000,
            retry_count: 0,
        });
        setModalOpen(true);
    };

    const handleEdit = (s: any) => {
        if (!canWrite() || modalOpen || clearOpen) return;
        setEditing(s);
        form.resetFields();
        form.setFieldsValue(s);
        setModalOpen(true);
    };

    // 接口全量替换：弹窗保存同时提交当前列表中的移动/删除草稿。
    const persist = async (nextSteps: any[]) => {
        const res = await suiteApi.saveSteps(Number(id), nextSteps);
        if (res?.success !== true) throw new Error(res?.msg || '保存失败');
        if (!active.current) return false;
        setSteps(nextSteps);
        setDirty(false);
        message.success('保存成功');
        return true;
    };

    const handleSave = async () => {
        if (!canWrite() || !modalOpen) return;
        savingRef.current = true;
        setSaving(true);
        try {
            const values = await form.validateFields();
            if (!active.current) return;
            const nextSteps = editing
                ? steps.map(s => s.step_no === editing.step_no ? {...s, ...values, step_no: s.step_no} : s)
                : [...steps, {
                    execution_mode: 'api', ...values,
                    step_no: steps.length > 0 ? Math.max(...steps.map(s => s.step_no)) + 1 : 1,
                }];
            if (await persist(nextSteps)) setModalOpen(false);
        } catch (e: any) {
            if (active.current && !e?.errorFields) message.error('保存失败: ' + (e?.message || e));
        } finally {
            if (active.current) {
                savingRef.current = false;
                setSaving(false);
            }
        }
    };

    const handleDelete = (no: number) => {
        if (!canWrite() || modalOpen || clearOpen) return;
        setSteps(steps.filter(s => s.step_no !== no).map((s, i) => ({...s, step_no: i + 1})));
        setDirty(true);
    };

    const handleMove = (no: number, dir: 1 | -1) => {
        if (!canWrite() || modalOpen || clearOpen) return;
        const idx = steps.findIndex(s => s.step_no === no);
        const target = idx + dir;
        if (idx < 0 || target < 0 || target >= steps.length) return;
        const newSteps = [...steps];
        [newSteps[idx], newSteps[target]] = [newSteps[target], newSteps[idx]];
        setSteps(newSteps.map((s, i) => ({...s, step_no: i + 1})));
        setDirty(true);
    };

    const handleSaveAll = async (confirmClear = false) => {
        if (!canWrite() || !dirty || modalOpen) return;
        if (steps.length === 0 && !confirmClear) {
            setClearOpen(true);
            return;
        }
        savingRef.current = true;
        setSaving(true);
        try {
            if (await persist(steps)) setClearOpen(false);
        } catch (e: any) {
            if (active.current) message.error('保存失败: ' + (e?.message || e));
        } finally {
            if (active.current) {
                savingRef.current = false;
                setSaving(false);
            }
        }
    };

    return (
        <div style={{padding: 0}}>
            <Card
                styles={{header: {padding: '12px 16px'}}}
                title={
                    <div style={{display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: 8}}>
                        <span style={{flex: '1 1 160px', minWidth: 0, whiteSpace: 'normal', overflowWrap: 'anywhere'}}>
                            {`套件步骤 — ${String(suite?.id) === id ? suite.name : id}`}
                        </span>
                        <Space wrap>
                            {dirty && <Tag color="orange">未保存</Tag>}
                            <Button disabled={saving} onClick={() => navigate(`${qaRoot}/suites`)}>返回</Button>
                            <Button icon={<PlusOutlined/>} disabled={inlineDisabled} onClick={handleAdd}>新增步骤</Button>
                            <Button type="primary" icon={<SaveOutlined/>} disabled={inlineDisabled || !dirty}
                                    loading={saving && !modalOpen} onClick={() => handleSaveAll()}>保存全部</Button>
                        </Space>
                    </div>
                }
            >
                {loadState === 'error' && <Alert
                    type="error"
                    title={`步骤加载失败：${loadError}`}
                    action={<Button onClick={() => setRetry(n => n + 1)}>重试</Button>}
                    style={{marginBottom: 16}}
                    showIcon
                />}
                <Table
                    rowKey="step_no"
                    scroll={{x: 1200}}
                    loading={loadState === 'loading'}
                    dataSource={steps}
                    pagination={{
                        defaultPageSize: 20,
                        showSizeChanger: true,
                        pageSizeOptions: ['10', '20', '50', '100'],
                        showTotal: (t) => `共 ${t} 条`,
                    }}
                    size="small"
                    columns={[
                        {title: '#', dataIndex: 'step_no', width: 50},
                        {title: '名称', dataIndex: 'step_name', width: 180},
                        {
                            title: 'Method', dataIndex: 'method', width: 80,
                            render: t => <Tag color="blue">{t}</Tag>
                        },
                        {
                            title: 'URL', dataIndex: 'url', ellipsis: true,
                            render: t => t ? <code style={{fontSize: 11}}>{t}</code> : '-'
                        },
                        {
                            title: '目标', dataIndex: 'target_mode', width: 80,
                            render: t => <Tag color={t === 'real' ? 'green' : 'orange'}>{t}</Tag>
                        },
                        {
                            title: '提取', dataIndex: 'extract_rules',
                            render: t => t ? <code style={{fontSize: 10}}>{t}</code> : '-'
                        },
                        {
                            title: '断言', dataIndex: 'assert_rules',
                            render: t => t ? <code style={{fontSize: 10}}>{t}</code> : '-'
                        },
                        {title: '超时', dataIndex: 'timeout_ms', width: 80},
                        {
                            title: '重试', dataIndex: 'retry_count', width: 70,
                            render: v => v > 0
                                ? <Tooltip title="仅超时/连不上/5xx 会重试,断言不符不重试"><Tag color="blue">{v}</Tag></Tooltip>
                                : <Tag>0</Tag>
                        },
                        {
                            title: '操作', width: 200,
                            render: (_, s) => (
                                <Space>
                                    <Tooltip title="上移"><Button type="link" size="small"
                                                                  disabled={inlineDisabled || s.step_no === steps[0]?.step_no}
                                                                  onClick={() => handleMove(s.step_no, -1)}>上移</Button></Tooltip>
                                    <Tooltip title="下移"><Button type="link" size="small"
                                                                  disabled={inlineDisabled || s.step_no === steps[steps.length - 1]?.step_no}
                                                                  onClick={() => handleMove(s.step_no, 1)}>下移</Button></Tooltip>
                                    <Button type="link" size="small" disabled={inlineDisabled}
                                            onClick={() => handleEdit(s)}>编辑</Button>
                                    <Popconfirm title="确认删除该步骤？" description="删除后需保存全部才会生效。"
                                                okText="删除" cancelText="取消" okButtonProps={{danger: true}}
                                                disabled={inlineDisabled} onConfirm={() => handleDelete(s.step_no)}>
                                        <Button type="link" size="small" danger disabled={inlineDisabled}>删除</Button>
                                    </Popconfirm>
                                </Space>
                            )
                        },
                    ]}
                />
            </Card>

            <Modal
                title={editing ? `编辑步骤 #${editing.step_no}` : '新增步骤'}
                open={modalOpen}
                onCancel={() => { if (!savingRef.current) setModalOpen(false); }}
                onOk={handleSave}
                okText="保存"
                confirmLoading={saving}
                okButtonProps={{disabled: loadState !== 'ready' || saving}}
                cancelButtonProps={{disabled: saving}}
                closable={!saving}
                maskClosable={!saving}
                keyboard={!saving}
                destroyOnHidden
            >
                <Form form={form} layout="vertical" disabled={loadState !== 'ready' || saving}>
                    <Form.Item name="step_name" label="步骤名称" rules={[{required: true}]}>
                        <Input placeholder="如: 登录获取 token"/>
                    </Form.Item>
                    <Form.Item name="method" label="HTTP Method" rules={[{required: true}]}>
                        <Select>
                            <Select.Option value="GET">GET</Select.Option>
                            <Select.Option value="POST">POST</Select.Option>
                            <Select.Option value="PUT">PUT</Select.Option>
                            <Select.Option value="DELETE">DELETE</Select.Option>
                        </Select>
                    </Form.Item>
                    <Form.Item name="url" label="URL (相对或绝对, 支持 ${VAR})" rules={[{required: true}]}>
                        <Input placeholder="/api/ctc/authn/login 或 https://example.com/api"/>
                    </Form.Item>
                    <Form.Item name="headers_json" label="Headers (JSON)">
                        <Input.TextArea rows={2} placeholder='{"Content-Type": "application/json"}'/>
                    </Form.Item>
                    <Form.Item name="body" label="Body (支持 ${VAR})">
                        <Input.TextArea rows={4} placeholder='{"username":"admin","password":"admin"}'/>
                    </Form.Item>
                    <Form.Item name="target_mode" label="目标模式">
                        <Select>
                            <Select.Option value="real">real (真实服务)</Select.Option>
                            <Select.Option value="mock">mock (Mock 端点)</Select.Option>
                        </Select>
                    </Form.Item>
                    <Form.Item name="extract_rules" label="变量提取 (JSONPath → 变量名)">
                        <Input.TextArea rows={2} placeholder='{"token": "$.data.token"}'/>
                    </Form.Item>
                    <Form.Item name="assert_rules" label="断言规则">
                        <Input.TextArea rows={2} placeholder='{"status": 200, "$.code": 200}'/>
                    </Form.Item>
                    <Form.Item name="timeout_ms" label="超时 (ms)">
                        <InputNumber min={1000} max={60000}/>
                    </Form.Item>
                    <Form.Item name="retry_count" label="失败重试">
                        <InputNumber min={0} max={5}/>
                    </Form.Item>
                </Form>
            </Modal>
            <Modal
                title="确认清空全部步骤？"
                open={clearOpen}
                onOk={() => handleSaveAll(true)}
                onCancel={() => { if (!savingRef.current) setClearOpen(false); }}
                okText="确认清空"
                cancelText="取消"
                confirmLoading={saving}
                okButtonProps={{danger: true, disabled: loadState !== 'ready' || saving}}
                cancelButtonProps={{disabled: saving}}
                closable={!saving}
                maskClosable={!saving}
                keyboard={!saving}
            >
                保存空列表将删除该套件在服务端的全部步骤，此操作不可撤销。
            </Modal>
        </div>
    );
};

export default SuiteSteps;