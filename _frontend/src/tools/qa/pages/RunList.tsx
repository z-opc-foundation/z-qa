import React, {useEffect, useState} from 'react';
import {Button, Card, Progress, Space, Table, Tag, Tooltip} from 'antd';
import {EyeOutlined, FileSearchOutlined, ReloadOutlined} from '@ant-design/icons';
import {runApi} from '../services/api';
import {useNavigate} from 'react-router-dom';

/**
 * 执行记录列表 + 实时状态轮询.
 * 路径: /tools/qa/runs
 */
const RunList: React.FC = () => {
    const [runs, setRuns] = useState<any[]>([]);
    const [loading, setLoading] = useState(false);
    const navigate = useNavigate();

    const load = async () => {
        setLoading(true);
        try {
            const res = await runApi.list({limit: 100});
            setRuns(res?.content || []);
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        load();
        const timer = setInterval(load, 5000);  // 5s 轮询
        return () => clearInterval(timer);
    }, []);

    const statusColors: Record<string, string> = {
        success: 'success', failed: 'error', running: 'processing', pending: 'default', cancelled: 'warning',
    };

    return (
        <div style={{padding: 24}}>
            <Card
                title={<Space><FileSearchOutlined/> 执行记录</Space>}
                extra={<Button icon={<ReloadOutlined/>} onClick={load}>刷新</Button>}
            >
                <Table
                    rowKey="id"
                    dataSource={runs}
                    loading={loading}
                    pagination={{pageSize: 20}}
                    columns={[
                        {
                            title: 'Run Code', dataIndex: 'run_code', width: 220,
                            render: t => <code style={{fontSize: 11}}>{t}</code>
                        },
                        {title: '套件', dataIndex: 'name', ellipsis: true},
                        {
                            title: '触发方式', dataIndex: 'trigger_type', width: 100,
                            render: t => <Tag>{t}</Tag>
                        },
                        {
                            title: '状态', dataIndex: 'status', width: 100,
                            render: s => <Tag color={statusColors[s]}>{s}</Tag>
                        },
                        {
                            title: '进度', width: 200,
                            render: (_, r: any) => {
                                const total = Number(r.total_steps) || 0;
                                const done = (Number(r.passed_steps) || 0) + (Number(r.failed_steps) || 0);
                                const percent = total > 0 ? Math.round((done / total) * 100) : 0;
                                return (
                                    <Tooltip title={`${done}/${total}`}>
                                        <Progress percent={percent} size="small"
                                                  status={r.status === 'failed' ? 'exception' :
                                                      r.status === 'success' ? 'success' :
                                                          r.status === 'running' ? 'active' : 'normal'}/>
                                    </Tooltip>
                                );
                            }
                        },
                        {
                            title: '通过/失败', width: 110,
                            render: (_, r: any) => (
                                <Space>
                                    <Tag color="success">{r.passed_steps || 0}</Tag>
                                    <Tag color={r.failed_steps > 0 ? 'error' : 'default'}>{r.failed_steps || 0}</Tag>
                                </Space>
                            )
                        },
                        {
                            title: '耗时', dataIndex: 'duration_ms', width: 90,
                            render: t => t ? `${t}ms` : '-'
                        },
                        {title: '触发人', dataIndex: 'trigger_user', width: 100},
                        {
                            title: '操作', width: 100, fixed: 'right',
                            render: (_, r) => (
                                <Button size="small" icon={<EyeOutlined/>}
                                        onClick={() => navigate(`/test/qa/runs/${r.id}`)}>详情</Button>
                            )
                        },
                    ]}
                />
            </Card>
        </div>
    );
};

export default RunList;