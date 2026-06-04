import React, { useEffect, useMemo, useState } from 'react';
import { createRoot } from 'react-dom/client';
import { Bar, Line, Pie } from 'react-chartjs-2';
import { Chart, ArcElement, BarElement, CategoryScale, LinearScale, PointElement, LineElement, Tooltip, Legend } from 'chart.js';
import 'bootstrap/dist/css/bootstrap.min.css';
import './styles/app.css';
import api, { setToken } from './services/api';

Chart.register(ArcElement, BarElement, CategoryScale, LinearScale, PointElement, LineElement, Tooltip, Legend);

const roles = ['EMPLOYEE', 'ADMIN', 'HQ', 'CLIENT'];
const defaultSubsidiaries = [
  { id: 1, code: 'ECL', name: 'Eastern Coalfields Limited' },
  { id: 2, code: 'BCCL', name: 'Bharat Coking Coal Limited' },
  { id: 3, code: 'CCL', name: 'Central Coalfields Limited' },
  { id: 4, code: 'WCL', name: 'Western Coalfields Limited' },
  { id: 5, code: 'SECL', name: 'South Eastern Coalfields Limited' },
  { id: 6, code: 'NCL', name: 'Northern Coalfields Limited' },
  { id: 7, code: 'MCL', name: 'Mahanadi Coalfields Limited' },
  { id: 8, code: 'CMPDI', name: 'Central Mine Planning & Design Institute Limited' }
];
const modules = {
  EMPLOYEE: ['attendance', 'leave', 'scholarship', 'buscard', 'pension', 'notifications'],
  ADMIN: ['attendance', 'leave', 'scholarship', 'buscard', 'pension', 'inventory', 'orders'],
  HQ: ['attendance', 'leave', 'scholarship', 'buscard', 'pension', 'inventory', 'orders', 'reports'],
  CLIENT: ['inventory', 'orders', 'notifications']
};

function App() {
  const [session, setSession] = useState(() => JSON.parse(localStorage.getItem('civwbms.session') || 'null'));
  const [theme, setTheme] = useState(localStorage.getItem('civwbms.theme') || 'light');
  useEffect(() => { document.body.dataset.theme = theme; localStorage.setItem('civwbms.theme', theme); }, [theme]);
  useEffect(() => { if (session?.token) setToken(session.token); }, [session]);

  if (!session) return <Auth onAuth={s => { localStorage.setItem('civwbms.session', JSON.stringify(s)); setSession(s); }} />;
  return <Shell session={session} theme={theme} setTheme={setTheme} logout={() => { localStorage.clear(); setSession(null); }} />;
}

function Auth({ onAuth }) {
  const [mode, setMode] = useState('login');
  const [role, setRole] = useState('EMPLOYEE');
  const [subs, setSubs] = useState([]);
  const [form, setForm] = useState({});
  const [error, setError] = useState('');
  useEffect(() => {
    api.get('/subsidiaries')
      .then(r => setSubs(r.data.length ? r.data : defaultSubsidiaries))
      .catch(() => setSubs(defaultSubsidiaries));
  }, []);
  useEffect(() => {
    if (subs.length && !form.subsidiaryId) {
      const defaultSub = subs.find(s => s.code === 'ECL') || subs.find(s => Number(s.id) === 1) || subs[0];
      setForm(f => ({ ...f, subsidiaryId: defaultSub.id }));
    }
  }, [subs, form.subsidiaryId]);

  async function submit(e) {
    e.preventDefault();
    setError('');
    try {
      const payload = { role, identifier: form.identifier, password: form.password, subsidiaryId: role === 'CLIENT' ? null : Number(form.subsidiaryId), profile: form };
      const { data } = await api.post(`/auth/${mode}`, payload);
      onAuth(data);
    } catch (err) {
      setError(err.response?.data?.message || 'Backend server is not reachable. Start the backend and try again.');
    }
  }

  return <main className="auth-screen">
    <section className="auth-panel">
      <div className="brand-mark">CIL</div>
      <h1>Vendor Management System</h1>
      <p>Employee welfare, coal inventory, client booking, and HQ analytics on one secure platform.</p>
      {error && <div className="alert alert-danger">{error}</div>}
      <form onSubmit={submit} className="card form-card">
        <div className="btn-group mb-3">{roles.map(r => <button type="button" className={`btn btn-sm ${role === r ? 'btn-warning' : 'btn-outline-secondary'}`} onClick={() => setRole(r)} key={r}>{r}</button>)}</div>
        <div className="row g-3">
          <Field label={`${role === 'CLIENT' ? 'Client' : role === 'HQ' ? 'HQ' : 'Employee'} ID`} name="identifier" setForm={setForm} />
          {role !== 'CLIENT' && <div className="col-md-6"><label className="form-label">Subsidiary</label><select required className="form-select" value={form.subsidiaryId || ''} onChange={e => setForm(f => ({ ...f, subsidiaryId: e.target.value }))}><option value="" disabled>Select</option>{subs.map(s => <option value={s.id} key={s.id}>{s.code}</option>)}</select></div>}
          <Field label="Password" name="password" type="password" setForm={setForm} />
          {mode === 'register' && <RegisterFields role={role} setForm={setForm} />}
        </div>
        <button className="btn btn-warning w-100 mt-3">{mode === 'login' ? 'Login' : 'Create Account'}</button>
        <button type="button" className="btn btn-link" onClick={() => setMode(mode === 'login' ? 'register' : 'login')}>{mode === 'login' ? 'Need registration?' : 'Already registered?'}</button>
      </form>
    </section>
  </main>;
}

function RegisterFields({ role, setForm }) {
  if (role === 'CLIENT') return <>
    <Field label="Company Name" name="companyName" setForm={setForm} />
    <Field label="GST Number" name="gstNumber" setForm={setForm} />
    <Field label="Contact Person" name="contactPerson" setForm={setForm} />
    <Field label="Email" name="email" type="email" setForm={setForm} />
    <Field label="Mobile" name="mobile" setForm={setForm} />
  </>;
  return <>
    <Field label={role === 'EMPLOYEE' ? 'Full Name' : 'Name'} name={role === 'EMPLOYEE' ? 'fullName' : 'name'} setForm={setForm} />
    <Field label="Email" name="email" type="email" setForm={setForm} />
    <Field label="Mobile" name="mobile" setForm={setForm} />
  </>;
}

function Shell({ session, theme, setTheme, logout }) {
  const role = session.user.role;
  const [active, setActive] = useState('dashboard');
  const [stats, setStats] = useState(null);
  useEffect(() => { api.get('/dashboard').then(r => setStats(r.data)); }, [active]);
  return <div className="app-shell">
    <aside className="sidebar">
      <div className="brand"><span>CIL</span><strong>CIVWBMS</strong></div>
      <button className={active === 'dashboard' ? 'active' : ''} onClick={() => setActive('dashboard')}>Dashboard</button>
      {modules[role].map(m => <button key={m} className={active === m ? 'active' : ''} onClick={() => setActive(m)}>{label(m)}</button>)}
    </aside>
    <section className="workspace">
      <nav className="topbar">
        <div><strong>{session.user.name}</strong><small>{role}</small></div>
        <div className="d-flex gap-2"><button className="btn btn-sm btn-outline-dark" onClick={() => setTheme(theme === 'dark' ? 'light' : 'dark')}>{theme === 'dark' ? 'Light' : 'Dark'}</button><button className="btn btn-sm btn-dark" onClick={logout}>Logout</button></div>
      </nav>
      {active === 'dashboard' ? <Dashboard stats={stats} role={role} /> : active === 'reports' ? <Reports /> : <Module module={active} role={role} />}
    </section>
  </div>;
}

function Dashboard({ stats, role }) {
  const cards = stats ? Object.entries(stats).filter(([k]) => k !== 'charts') : [];
  return <div>
    <h2>{role} Dashboard</h2>
    <div className="metric-grid">{cards.map(([k, v]) => <div className="metric" key={k}><span>{label(k)}</span><strong>{String(v)}</strong></div>)}</div>
    {stats?.charts && <div className="chart-grid">
      <ChartCard title="Coal Quality"><Pie data={chartData(stats.charts.quality)} /></ChartCard>
      <ChartCard title="Order Status"><Bar data={chartData(stats.charts.orders)} /></ChartCard>
      <ChartCard title="Monthly Orders"><Line data={chartData(stats.charts.monthly)} /></ChartCard>
    </div>}
  </div>;
}

function Module({ module, role }) {
  const [rows, setRows] = useState([]);
  const [inventoryRows, setInventoryRows] = useState([]);
  const [subs, setSubs] = useState([]);
  const [form, setForm] = useState({});
  const [message, setMessage] = useState('');
  const canCreate = role === 'EMPLOYEE' || (role === 'ADMIN' && module === 'inventory') || (role === 'CLIENT' && module === 'orders');
  const canApprove = role === 'ADMIN' && ['attendance', 'leave', 'scholarship', 'buscard', 'pension', 'orders'].includes(module);
  useEffect(() => { load(); api.get('/subsidiaries').then(r => setSubs(r.data)); if (module === 'orders') api.get('/inventory').then(r => setInventoryRows(r.data)); }, [module]);
  async function load() { const { data } = await api.get(`/${module}`); setRows(data); }
  async function save(e) { e.preventDefault(); await api.post(`/${module}`, form); setForm({}); setMessage('Saved successfully'); load(); }
  async function approve(id, status) { const remarks = prompt('Approval remarks') || status; await api.put(`/${module}/${id}`, { status, remarks }); load(); }
  async function remove(id) { if (confirm('Delete inventory record?')) { await api.delete(`/${module}/${id}`); load(); } }
  return <div>
    <div className="d-flex justify-content-between align-items-center mb-3"><h2>{label(module)}</h2><input className="form-control search" placeholder="Search table" onChange={e => filterRows(e.target.value)} /></div>
    {message && <div className="alert alert-success">{message}</div>}
    {canCreate && <Form module={module} rows={module === 'orders' ? inventoryRows : rows} subs={subs} form={form} setForm={setForm} save={save} />}
    <DataTable rows={rows} actions={(r) => <>{canApprove && <><button className="btn btn-sm btn-success me-1" onClick={() => approve(r.id, module === 'orders' ? 'Approved' : 'Approved')}>Approve</button><button className="btn btn-sm btn-danger me-1" onClick={() => approve(r.id, module === 'orders' ? 'Cancelled' : 'Rejected')}>Reject</button>{module === 'orders' && <button className="btn btn-sm btn-warning me-1" onClick={() => approve(r.id, 'Dispatched')}>Dispatch</button>}</>}{module === 'inventory' && role === 'ADMIN' && <button className="btn btn-sm btn-outline-danger" onClick={() => remove(r.id)}>Delete</button>}</>} />
  </div>;
}

function Form({ module, rows, subs, form, setForm, save }) {
  const inv = rows.filter(r => r.available_quantity > 0);
  return <form onSubmit={save} className="card form-card mb-3"><div className="row g-3">
    {module === 'attendance' && <><Field label="Date" name="attendanceDate" type="date" setForm={setForm} /><Field label="In Time" name="inTime" type="time" setForm={setForm} /><Field label="Out Time" name="outTime" type="time" setForm={setForm} /></>}
    {module === 'leave' && <><Select label="Leave Type" name="leaveType" setForm={setForm} options={['Casual Leave','Earned Leave','Half Pay Leave','Sick Leave','Maternity Leave','Child Care Leave','Paternity Leave','Study Leave']} /><Field label="From Date" name="fromDate" type="date" setForm={setForm} /><Field label="To Date" name="toDate" type="date" setForm={setForm} /><Field label="Reason" name="reason" setForm={setForm} /><Field label="Document Path" name="documentPath" setForm={setForm} /></>}
    {module === 'scholarship' && <><Field label="Student Name" name="studentName" setForm={setForm} /><Field label="Relationship" name="relationship" setForm={setForm} /><Field label="School/College" name="schoolCollege" setForm={setForm} /><Field label="Class" name="className" setForm={setForm} /><Field label="Marks" name="marks" type="number" setForm={setForm} /><Field label="Annual Income" name="annualIncome" type="number" setForm={setForm} /><Field label="Document Path" name="documentPath" setForm={setForm} /></>}
    {module === 'buscard' && <><Field label="Student Name" name="studentName" setForm={setForm} /><Field label="School Name" name="schoolName" setForm={setForm} /><Field label="Route" name="route" setForm={setForm} /><Field label="Address" name="address" setForm={setForm} /><Field label="Photo Path" name="photoPath" setForm={setForm} /></>}
    {module === 'pension' && <><Field label="Retirement Date" name="retirementDate" type="date" setForm={setForm} /><Field label="Service Years" name="serviceYears" type="number" setForm={setForm} /><Field label="Last Designation" name="lastDesignation" setForm={setForm} /><Field label="Document Path" name="documentPath" setForm={setForm} /></>}
    {module === 'inventory' && <><Select label="Subsidiary" name="subsidiaryId" setForm={setForm} options={subs.map(s => [s.id, `${s.code} - ${s.name}`])} /><Field label="Area" name="area" setForm={setForm} /><Field label="Colliery" name="colliery" setForm={setForm} /><Field label="Coal Grade" name="coalGrade" setForm={setForm} /><Select label="Coal Quality" name="coalQuality" setForm={setForm} options={['Premium','Good','Medium','Low']} /><Field label="Available Quantity" name="availableQuantity" type="number" setForm={setForm} /><Field label="Price Per Tonne" name="pricePerTonne" type="number" setForm={setForm} /></>}
    {module === 'orders' && <><Select label="Coal Stock" name="inventoryId" setForm={setForm} options={inv.map(i => [i.id, `${i.subsidiary_code} ${i.area} ${i.colliery} ${i.coal_grade} - ${i.available_quantity}T`])} /><Field label="Quantity Required" name="quantityRequired" type="number" setForm={setForm} /><Field label="Delivery Address" name="deliveryAddress" setForm={setForm} /><Field label="GST Number" name="gstNumber" setForm={setForm} /></>}
  </div><button className="btn btn-warning mt-3">Submit</button></form>;
}

function Reports() {
  const reports = ['approved-welfare', 'inventory', 'orders'];
  async function download(name, type) {
    const ext = type === 'pdf' ? 'pdf' : 'xlsx';
    const { data } = await api.get(`/reports/${name}/${type}`, { responseType: 'blob' });
    const url = URL.createObjectURL(data);
    const a = document.createElement('a');
    a.href = url;
    a.download = `${name}.${ext}`;
    a.click();
    URL.revokeObjectURL(url);
  }
  return <div><h2>HQ Reports</h2><div className="row g-3">{reports.map(r => <div className="col-md-4" key={r}><div className="card report-card"><h5>{label(r)}</h5><button className="btn btn-sm btn-danger me-2" onClick={() => download(r, 'pdf')}>PDF</button><button className="btn btn-sm btn-success" onClick={() => download(r, 'excel')}>Excel</button></div></div>)}</div></div>;
}

function DataTable({ rows, actions }) {
  const headers = rows[0] ? Object.keys(rows[0]) : [];
  return <div className="table-responsive card table-card"><table className="table table-hover align-middle"><thead><tr>{headers.map(h => <th key={h}>{label(h)}</th>)}<th>Actions</th></tr></thead><tbody>{rows.map(r => <tr key={r.id || JSON.stringify(r)}>{headers.map(h => <td key={h}>{String(r[h] ?? '')}</td>)}<td>{actions(r)}</td></tr>)}</tbody></table>{rows.length === 0 && <div className="empty">No records found</div>}</div>;
}

function Field({ label, name, type = 'text', setForm }) {
  return <div className="col-md-6"><label className="form-label">{label}</label><input required className="form-control" type={type} onChange={e => setForm(f => ({ ...f, [name]: e.target.value }))} /></div>;
}

function Select({ label, name, options, setForm }) {
  return <div className="col-md-6"><label className="form-label">{label}</label><select required className="form-select" onChange={e => setForm(f => ({ ...f, [name]: e.target.value }))}><option value="">Select</option>{options.map(o => Array.isArray(o) ? <option value={o[0]} key={o[0]}>{o[1]}</option> : <option value={o} key={o}>{o}</option>)}</select></div>;
}

function ChartCard({ title, children }) {
  return <div className="card chart-card"><h5>{title}</h5>{children}</div>;
}

function chartData(rows = []) {
  return { labels: rows.map(r => r.label), datasets: [{ label: 'Count', data: rows.map(r => r.value), backgroundColor: ['#f2b705','#2f5233','#1d3557','#d1495b','#6c757d'], borderColor: '#2b2b2b' }] };
}

function label(s) {
  return String(s).replaceAll('_', ' ').replaceAll('-', ' ').replace(/\b\w/g, c => c.toUpperCase());
}

function filterRows(q) {
  document.querySelectorAll('tbody tr').forEach(tr => tr.style.display = tr.innerText.toLowerCase().includes(q.toLowerCase()) ? '' : 'none');
}

createRoot(document.getElementById('root')).render(<App />);
