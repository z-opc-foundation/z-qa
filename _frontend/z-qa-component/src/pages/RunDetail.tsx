import React, {useEffect, useState} from 'react';
import {Alert, Button, Card, Col, Descriptions, message, Popconfirm, Row, Space, Statistic, Steps, Table, Tabs, Tag, Tooltip} from 'antd';
import {
    ArrowLeftOutlined,
    CheckCircleOutlined,
    ClockCircleOutlined,
    CloseCircleOutlined,
    FileTextOutlined,
    MinusCircleOutlined,
    ReloadOutlined,
    StopOutlined
} from '@ant-design/icons';
import {runApi} from '../services/api';
import {defectLabel, defectTagColor, isFailStatus, stepStatusLabel, stepStatusTagColor} from '../services/stepStatus';
import {useNavigate, useParams} from 'react-router-dom';

/**
 * 执行详情 + 全链路报告 (含每步请求/响应/断言).
 * 路径: /tools/qa/runs/:id
 */
const RunDetail: React.FC = () => {
    const {id} = useParams<{ id: string }>();
    const navigate = useNavigate();
    const [run, setRun] = useState<any>(null);
    const [details, setDetails] = useState<any[]>([]);
    const [status, setStatus] = useState<any>(null);
    const [loading, setLoading] = useState(true);

    const load = async () => {
        try {
            const [r, d, s] = await Promise.all([
                runApi.get(parseInt(id!)),
                runApi.details(parseInt(id!)),
                runApi.status(parseInt(id!)),
            ]);
            setRun(r?.content);
            setDetails(d?.content || []);
            setStatus(s?.content);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        load();
        const timer = setInterval(() => {
            if (status?.status === 'running' || status?.status === 'pending') {
                load();
            }
        }, 3000);
        return () => clearInterval(timer);
    }, [id, status?.status]);

    if (loading || !run) {
        return <div style={{padding: 0}}>加载中...</div>;
    }

    const RUN_STATUS_HEX: Record<string, string> = {
        success: '#3f8600', failed: '#cf1322', cancelled: '#d48806', running: '#1890ff', pending: '#6e7781',
    };

    const runStatus = status?.status || run.status;
    const abortable = runStatus === 'running' || runStatus === 'pending';

    const handleAbort = async () => {
        try {
            const res = await runApi.abort(parseInt(id!));
            if (res?.code === 200) message.success('已请求中止，剩余步骤将记为跳过');
            else message.warning(res?.msg || '中止失败');
        } catch (e: any) {
            message.error('中止失败: ' + (e?.message || e));
        } finally {
            load();
        }
    };

    const passRate = run.total_steps > 0 ? Math.round((run.passed_steps / run.total_steps) * 100) : 0;
    const totalDuration = run.duration_ms || 0;

    return (
        <div style={{padding: 0}}>
            <Space style={{marginBottom: 16}}>
                <Button icon={<ArrowLeftOutlined/>} onClick={() => navigate('/z-qa/runs')}>返回</Button>
                <Button icon={<ReloadOutlined/>} onClick={load}>刷新</Button>
                {abortable && (
                    <Popconfirm title="中止后剩余步骤记为跳过，确认？" okText="中止" okButtonProps={{danger: true}}
                                onConfirm={handleAbort}>
                        <Tooltip title={status?.abortRequested ? '已请求中止，等待执行线程收尾' : undefined}>
                            <Button danger icon={<StopOutlined/>}
                                    disabled={status?.abortRequested}>中止执行</Button>
                        </Tooltip>
                    </Popconfirm>
                )}
            </Space>

            <Card title={<Space><FileTextOutlined/> 执行详情 — {run.run_code}</Space>} style={{marginBottom: 16}}>
                <Row gutter={16}>
                    <Col span={4}><Statistic title="套件名称" value={run.name || '-'}/></Col>
                    <Col span={4}><Statistic title="状态" value={runStatus}
                                             styles={{content: {color: RUN_STATUS_HEX[runStatus] || '#1890ff'}}}/></Col>
                    <Col span={4}><Statistic title="总步骤" value={run.total_steps || 0}/></Col>
                    <Col span={4}><Statistic title="通过率" value={passRate} suffix="%"
                                             styles={{content: {color: passRate >= 80 ? '#3f8600' : '#cf1322'}}}/></Col>
                    <Col span={4}><Statistic title="耗时" value={totalDuration} suffix="ms"/></Col>
                    <Col span={4}><Statistic title="触发" value={run.trigger_user || '-'}/></Col>
                </Row>
            </Card>

            {/* 链路可视化 - 用 Steps 显示 */}
            <Card title="执行链路" style={{marginBottom: 16}}>
                {details.length > 0 ? (
                    <Steps
                        progressDot
                        current={details.length}
                        direction="horizontal"
                        status={runStatus === 'failed' ? 'error' : runStatus === 'success' ? 'finish' : 'process'}
                        items={details.map((d: any) => ({
                            title: `#${d.step_no} ${d.step_name}`,
                            description: (
                                <Space orientation="vertical" size={0}>
                                    <Tag color={stepStatusTagColor(d.result)}>{stepStatusLabel(d.result)}</Tag>
                                    <span style={{fontSize: 11}}>{d.method} {d.request_url}</span>
                                    {d.duration_ms &&
                                        <span style={{fontSize: 11, color: '#999'}}>{d.duration_ms}ms</span>}
                                </Space>
                            ),
                            status: d.result === 'success' ? 'finish' :
                                isFailStatus(d.result) ? 'error' :
                                    d.result === 'skip' ? 'wait' : 'process',
                            icon: d.result === 'success' ? <CheckCircleOutlined/> :
                                isFailStatus(d.result) ? <CloseCircleOutlined/> :
                                    d.result === 'skip' ? <MinusCircleOutlined/> :
                                        <ClockCircleOutlined/>,
                        }))}
                    />
                ) : (
                    <Alert type="info" title="暂无步骤详情"/>
                )}
            </Card>

            {/* 步骤详情表 */}
            <Card title="步骤详情">
                <Table
                    rowKey="id"
                    dataSource={details}
                    pagination={false}
                    size="small"
                    expandable={{
                        expandedRowRender: (d: any) => (
                            <Tabs
                                size="small"
                                items={[
                                    {
                                        key: 'req', label: '请求',
                                        children: (
                                            <pre style={{
                                                background: '#fafafa',
                                                padding: 8,
                                                fontSize: 11,
                                                maxHeight: 300,
                                                overflow: 'auto'
                                            }}>
                                                {`${d.method} ${d.request_url}\n\nHeaders:\n${d.request_headers || '-'}\n\nBody:\n${d.request_body || '-'}`}
                                            </pre>
                                        ),
                                    },
                                    {
                                        key: 'resp', label: `响应 (${d.response_status || '-'})`,
                                        children: (
                                            <pre style={{
                                                background: '#fafafa',
                                                padding: 8,
                                                fontSize: 11,
                                                maxHeight: 400,
                                                overflow: 'auto'
                                            }}>
                                                {d.response_body || '(empty)'}
                                            </pre>
                                        ),
                                    },
                                    {
                                        key: 'assert', label: '断言',
                                        children: (
                                            <pre style={{background: '#fafafa', padding: 8, fontSize: 11}}>
                                                {d.assert_results || '-'}
                                            </pre>
                                        ),
                                    },
                                    {
                                        key: 'vars', label: '变量',
                                        children: (
                                            <Descriptions size="small" column={1} bordered>
                                                <Descriptions.Item
                                                    label="执行前">{d.variables_before}</Descriptions.Item>
                                                <Descriptions.Item label="提取">{d.extracted_vars}</Descriptions.Item>
                                                <Descriptions.Item label="执行后">{d.variables_after}</Descriptions.Item>
                                            </Descriptions>
                                        ),
                                    },
                                ]}
                            />
                        ),
                    }}
                    columns={[
                        {title: '#', dataIndex: 'step_no', width: 50},
                        {title: '步骤', dataIndex: 'step_name', width: 180},
                        {
                            title: 'Method', dataIndex: 'method', width: 80,
                            render: t => t ? <Tag color="blue">{t}</Tag> : '-'
                        },
                        {
                            title: 'URL', dataIndex: 'request_url', ellipsis: true,
                            render: t => t ? <code style={{fontSize: 11}}>{t}</code> : '-'
                        },
                        {
                            title: 'HTTP', dataIndex: 'response_status', width: 80,
                            render: s => s ? <Tag color={s >= 200 && s < 300 ? 'success' : 'error'}>{s}</Tag> : '-'
                        },
                        {
                            title: '结果', dataIndex: 'result', width: 110,
                            render: r => <Tag color={stepStatusTagColor(r)}>{stepStatusLabel(r)}</Tag>
                        },
                        {
                            title: '缺陷归类', dataIndex: 'defect_type', width: 110,
                            render: (t, r: any) => t
                                ? <Tooltip title={r.error_message}><Tag color={defectTagColor(t)}>{defectLabel(t)}</Tag></Tooltip>
                                : '-'
                        },
                        {
                            title: '重试', dataIndex: 'retry_count', width: 70,
                            render: v => v > 0 ? <Tag color="blue">{v}</Tag> : '-'
                        },
                        {
                            title: '耗时', dataIndex: 'duration_ms', width: 90,
                            render: t => t ? `${t}ms` : '-'
                        },
                    ]}
                />
            </Card>
        </div>
    );
};

export default RunDetail;