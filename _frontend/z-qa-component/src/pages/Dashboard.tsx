import React, {useEffect, useState} from 'react';
import {Card, Col, Progress, Row, Statistic, Table, Tag, Tooltip} from 'antd';
import {
    CheckCircleOutlined,
    CloseCircleOutlined,
    ExperimentOutlined,
    FileTextOutlined,
    ScheduleOutlined
} from '@ant-design/icons';
import {dashboardApi, runApi} from '../services/api';
import {defectLabel, defectTagColor} from '../services/stepStatus';

/**
 * 测试平台 Dashboard - 质量看板.
 * 路径: /tools/qa
 */
const Dashboard: React.FC = () => {
    const [stats, setStats] = useState<any>(null);
    const [recentRuns, setRecentRuns] = useState<any[]>([]);
    const [topFailures, setTopFailures] = useState<any[]>([]);
    const [loading, setLoading] = useState(true);
    const [loadError, setLoadError] = useState<string | null>(null);

    useEffect(() => {
        (async () => {
            try {
                const [s, r, f] = await Promise.all([
                    dashboardApi.stats(),
                    runApi.list({limit: 10}),
                    dashboardApi.topFailures(),
                ]);
                setStats(s?.content || {});
                setRecentRuns(r?.content || []);
                setTopFailures(f?.content || []);
            } catch (e: any) {
                // 2026-10-04: 原来这里只有 try/finally 没有 catch，Promise.all 任一接口失败
                // （例如后端未实现时三个接口全 404）会让 rejection 逃出去变成**未捕获的
                // AxiosError**，控制台红字且页面永远停在「加载中...」。这里兜住并给出可读状态。
                console.warn('[qa] dashboard 加载失败:', e?.message || e);
                setLoadError(e?.message || '加载失败');
            } finally {
                setLoading(false);
            }
        })();
    }, []);

    if (loadError) {
        return <div style={{padding: 16, color: 'rgba(0,0,0,0.45)'}}>加载失败：{loadError}</div>;
    }

    if (loading || !stats) {
        return <div style={{padding: 0}}>加载中...</div>;
    }

    // 后端 COUNT 出来的是字符串 ("1"+"10" 会变成拼接), 参与运算前必须转数字
    const num = (v: any) => Number(v) || 0;
    const byStatus: Record<string, number> = stats.byStatus || {};
    const success = num(byStatus.success);
    const failed = num(byStatus.failed);
    const running = num(byStatus.running);
    const pending = num(byStatus.pending);
    const cancelled = num(byStatus.cancelled);
    const total = success + failed + running + pending + cancelled;
    const passRate = total > 0 ? Math.round((success / total) * 100) : 0;

    const statusColors: Record<string, string> = {
        success: 'success', failed: 'error', running: 'processing', pending: 'default', cancelled: 'warning',
    };

    return (
        <div style={{padding: 0}}>
            <h2><ExperimentOutlined/> 测试平台 · 质量看板</h2>

            <Row gutter={16} style={{marginBottom: 16}}>
                <Col span={4}>
                    <Card>
                        <Statistic title="测试套件" value={num(stats.totalSuites)} prefix={<FileTextOutlined/>}/>
                    </Card>
                </Col>
                <Col span={4}>
                    <Card>
                        <Statistic title="测试计划" value={num(stats.totalPlans)} prefix={<ScheduleOutlined/>}/>
                    </Card>
                </Col>
                <Col span={4}>
                    <Card>
                        <Statistic title="测试环境" value={num(stats.totalEnvs)}/>
                    </Card>
                </Col>
                <Col span={4}>
                    <Card>
                        <Statistic title="执行总数" value={total}/>
                    </Card>
                </Col>
                <Col span={4}>
                    <Card>
                        <Statistic title="通过率" value={passRate} suffix="%"
                                   styles={{content: {color: passRate >= 80 ? '#3f8600' : '#cf1322'}}}/>
                    </Card>
                </Col>
                <Col span={4}>
                    <Card>
                        <Statistic title="失败执行" value={failed}
                                   styles={{content: {color: failed > 0 ? '#cf1322' : '#3f8600'}}}
                                   prefix={failed > 0 ? <CloseCircleOutlined/> : <CheckCircleOutlined/>}/>
                    </Card>
                </Col>
            </Row>

            <Row gutter={16} style={{marginBottom: 16}}>
                <Col span={12}>
                    <Card title="执行状态分布">
                        <Row gutter={16}>
                            <Col span={6}><Statistic title="成功" value={success}
                                                     styles={{content: {color: '#3f8600'}}}/></Col>
                            <Col span={6}><Statistic title="失败" value={failed}
                                                     styles={{content: {color: '#cf1322'}}}/></Col>
                            <Col span={6}><Statistic title="运行中" value={running}
                                                     styles={{content: {color: '#1890ff'}}}/></Col>
                            <Col span={6}><Statistic title="等待中" value={pending}/></Col>
                        </Row>
                        <Progress
                            percent={passRate}
                            status={passRate >= 80 ? 'success' : passRate >= 60 ? 'normal' : 'exception'}
                            style={{marginTop: 16}}
                        />
                    </Card>
                </Col>
                <Col span={12}>
                    <Card title="使用场景">
                        <p>💡 此平台用于对 <b>z-opc 自己</b> 做集成测试</p>
                        <p>📦 预设套件: <code>zopc-self-test</code> — admin/login → my-tenants → meta-app → jobinfo</p>
                        <p>🧪 操作: 在 <a href="/tools/qa/suites">套件管理</a> 触发 "运行"</p>
                    </Card>
                </Col>
            </Row>

            <Card title="Top 失败步骤" style={{marginBottom: 16}}>
                <Table
                    rowKey={(r: any) => `${r.step_no}-${r.step_name}-${r.defect_type}`}
                    dataSource={topFailures}
                    size="small"
                    pagination={false}
                    locale={{emptyText: '还没有失败步骤'}}
                    columns={[
                        {
                            title: '步骤', dataIndex: 'step_name',
                            render: (t, r: any) => `${r.step_no ?? '-'} · ${t || '-'}`
                        },
                        {
                            title: '缺陷归类', dataIndex: 'defect_type', width: 140,
                            render: t => t
                                ? <Tooltip title="同一失败态恒归同一类，口径来自 QaFailureClassifier"><Tag
                                    color={defectTagColor(t)}>{defectLabel(t)}</Tag></Tooltip>
                                : '-'
                        },
                        {
                            title: '失败次数', dataIndex: 'fail_count', width: 100,
                            render: n => <Tag color={n > 0 ? 'red' : 'default'}>{n}</Tag>
                        },
                        {
                            title: '最近出现', dataIndex: 'last_seen', width: 180,
                            render: t => t ? String(t).replace('T', ' ').slice(0, 19) : '-'
                        },
                    ]}
                />
            </Card>

            <Card title="最近执行 (Top 10)">
                <Table
                    rowKey="id"
                    dataSource={recentRuns}
                    size="small"
                    pagination={false}
                    columns={[
                        {title: 'Run Code', dataIndex: 'run_code', width: 220},
                        {title: '套件', dataIndex: 'name'},
                        {
                            title: '触发方式', dataIndex: 'trigger_type', width: 90,
                            render: t => <Tag>{t}</Tag>
                        },
                        {
                            title: '状态', dataIndex: 'status', width: 100,
                            render: s => <Tag color={statusColors[s]}>{s}</Tag>
                        },
                        {
                            title: '通过/总数', width: 110,
                            render: (_, r: any) => `${num(r.passed_steps)} / ${num(r.total_steps)}`
                        },
                        {
                            title: '失败', dataIndex: 'failed_steps', width: 70,
                            render: v => v > 0 ? <Tag color="red">{v}</Tag> : <Tag>0</Tag>
                        },
                        {
                            title: '耗时', dataIndex: 'duration_ms', width: 90,
                            render: t => t ? `${t}ms` : '-'
                        },
                        {title: '触发人', dataIndex: 'trigger_user', width: 100},
                    ]}
                />
            </Card>
        </div>
    );
};

export default Dashboard;