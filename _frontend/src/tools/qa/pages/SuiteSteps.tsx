import React, {useEffect, useState} from 'react';
import {Alert, Button, Card, Form, Input, InputNumber, message, Modal, Select, Space, Table, Tag, Tooltip} from 'antd';
import {ArrowDownOutlined, ArrowUpOutlined, DeleteOutlined, PlusOutlined, SaveOutlined} from '@ant-design/icons';
import {suiteApi} from '../services/api';
import {useLocation, useNavigate, useParams} from 'react-router-dom';

/**
 * 套件步骤编辑器 - 链路可视化雏形 (步骤列表, 后续可改 LogicFlow).
 * 路径: /tools/qa/suites/:id/steps
 */
const SuiteSteps: React.FC = () => {
    const {id} = useParams<{ id: string }>();
    const location = useLocation();
    const navigate = useNavigate();
    const suite = (location.state as any)?.suite;
    const [steps, setSteps] = useState<any[]>([]);
    const [editing, setEditing] = useState<any>(null);
    const [modalOpen, setModalOpen] = useState(false);
    const [form] = Form.useForm();

    useEffect(() => {
        if (id) {
            suiteApi.steps(parseInt(id)).then(r => {
                setSteps(r?.content || []);
            });
        }
    }, [id]);

    const handleAdd = () => {
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
        setEditing(s);
        form.setFieldsValue(s);
        setModalOpen(true);
    };

    const handleSave = async () => {
        try {
            const values = await form.validateFields();
            if (editing) {
                const newSteps = steps.map(s => s.step_no === editing.step_no ? {...values, step_no: editing.step_no} : s);
                setSteps(newSteps);
            } else {
                const newStepNo = steps.length > 0 ? Math.max(...steps.map(s => s.step_no)) + 1 : 1;
                setSteps([...steps, {...values, step_no: newStepNo}]);
            }
            setModalOpen(false);
        } catch (e: any) {
            if (e?.errorFields) return;
            message.error('保存失败');
        }
    };

    const handleDelete = (no: number) => {
        setSteps(steps.filter(s => s.step_no !== no).map((s, i) => ({...s, step_no: i + 1})));
    };

    const handleMove = (idx: number, dir: 1 | -1) => {
        const target = idx + dir;
        if (target < 0 || target >= steps.length) return;
        const newSteps = [...steps];
        [newSteps[idx], newSteps[target]] = [newSteps[target], newSteps[idx]];
        setSteps(newSteps.map((s, i) => ({...s, step_no: i + 1})));
    };

    const handleSaveAll = async () => {
        try {
            await suiteApi.saveSteps(parseInt(id), steps);
            message.success('保存成功');
        } catch (e: any) {
            message.error('保存失败: ' + (e?.message || e));
        }
    };

    return (
        <div style={{padding: 24}}>
            <Card
                title={`套件步骤 — ${suite?.name || id}`}
                extra={
                    <Space>
                        <Button onClick={() => navigate('/test/qa/suites')}>返回</Button>
                        <Button icon={<PlusOutlined/>} onClick={handleAdd}>新增步骤</Button>
                        <Button type="primary" icon={<SaveOutlined/>} onClick={handleSaveAll}>保存全部</Button>
                    </Space>
                }
            >
                <Alert
                    type="info"
                    title="步骤按顺序执行, 变量从 ${VAR} 形式引用上一步提取的 JSONPath 结果."
                    style={{marginBottom: 16}}
                    showIcon
                />
                <Table
                    rowKey="step_no"
                    dataSource={steps}
                    pagination={false}
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
                            render: (_, s, idx) => (
                                <Space>
                                    <Tooltip title="上移"><Button size="small" icon={<ArrowUpOutlined/>}
                                                                  onClick={() => handleMove(idx as number, -1)}/></Tooltip>
                                    <Tooltip title="下移"><Button size="small" icon={<ArrowDownOutlined/>}
                                                                  onClick={() => handleMove(idx as number, 1)}/></Tooltip>
                                    <Button size="small" onClick={() => handleEdit(s)}>编辑</Button>
                                    <Button size="small" danger icon={<DeleteOutlined/>}
                                            onClick={() => handleDelete(s.step_no)}/>
                                </Space>
                            )
                        },
                    ]}
                />
            </Card>

            <Modal
                title={editing ? `编辑步骤 #${editing.step_no}` : '新增步骤'}
                open={modalOpen}
                onCancel={() => setModalOpen(false)}
                onOk={handleSave}
                destroyOnHidden
                size="large"
            >
                <Form form={form} layout="vertical">
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
        </div>
    );
};

export default SuiteSteps;