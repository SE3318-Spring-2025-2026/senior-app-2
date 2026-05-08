import { useCallback, useEffect, useRef, useState, useMemo } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  uploadStudentWhitelist,
  getStudentWhitelist,
  deleteStudentWhitelistEntry,
} from '../services/api';
import './StudentManagement.css';

function formatDate(iso) {
  if (!iso) return '—';
  try {
    const d = new Date(iso);
    return Number.isNaN(d.getTime()) ? iso : d.toLocaleString('tr-TR');
  } catch { return String(iso); }
}

function StudentManagement() {
  const navigate = useNavigate();
  const [ids, setIds] = useState('');
  const [status, setStatus] = useState({ type: '', msg: '' });
  const [list, setList] = useState([]);
  const [listLoading, setListLoading] = useState(true);
  const [deletingId, setDeletingId] = useState(null);
  const [dropActive, setDropActive] = useState(false);
  const fileInputRef = useRef(null);

  // FİLTRELEME STATE'İ
  const [filters, setFilters] = useState({
    studentId: '',
    linked: '',
    addedBy: '',
    addedDate: ''
  });

  const loadList = useCallback(async () => {
    setListLoading(true);
    try {
      const data = await getStudentWhitelist();
      setList(Array.isArray(data) ? data : []);
    } catch (err) {
      console.error("Liste yüklenemedi:", err);
    } finally {
      setListLoading(false);
    }
  }, []);

  useEffect(() => { loadList(); }, [loadList]);

  // ANLIK FİLTRELEME MANTIĞI (Görseldeki ihtiyaca göre)
  const filteredList = useMemo(() => {
    return list.filter((item) => {
      const isLinked = item.linked ? 'yapıldı' : 'yapılmadı';
      return (
        (item.studentId || '').toLowerCase().includes(filters.studentId.toLowerCase()) &&
        isLinked.includes(filters.linked.toLowerCase()) &&
        (item.addedBy || '').toLowerCase().includes(filters.addedBy.toLowerCase()) &&
        formatDate(item.addedDate).toLowerCase().includes(filters.addedDate.toLowerCase())
      );
    });
  }, [list, filters]);

  const handleFilterChange = (e) => {
    const { name, value } = e.target;
    setFilters(prev => ({ ...prev, [name]: value }));
  };

  const runUpload = async (idArray) => {
    const unique = [...new Set(idArray.map((s) => s.trim()).filter((s) => s !== ''))];
    if (unique.length === 0) return;
    try {
      const res = await uploadStudentWhitelist(unique);
      setStatus({ type: 'success', msg: `${res.added} yeni kayıt eklendi.` });
      setIds('');
      await loadList();
    } catch (err) {
      setStatus({ type: 'error', msg: 'Yükleme başarısız.' });
    }
  };

  const handleUpload = async (e) => {
    e.preventDefault();
    const idArray = ids.split(/[\n,]+/).map((s) => s.trim()).filter((s) => s !== '');
    await runUpload(idArray);
  };

  const onDelete = async (row) => {
    if (row.linked) return;
    if (!window.confirm(`Silmek istiyor musunuz? ${row.studentId}`)) return;
    setDeletingId(row.id);
    try {
      await deleteStudentWhitelistEntry(row.id);
      await loadList();
    } catch (err) {
      alert("Silinemedi.");
    } finally {
      setDeletingId(null);
    }
  };

  return (
    <div className="users-page">
      <h1>Öğrenci Beyaz Liste</h1>
      
      {/* CSV Yükleme ve Manuel Giriş Alanı (Mevcut kodun aynısı buraya gelecek) */}
      <div className="add-user-section">
        <form onSubmit={handleUpload}>
           <textarea 
            className="student-mgmt-textarea"
            placeholder="Öğrenci numaralarını girin..."
            value={ids}
            onChange={(e) => setIds(e.target.value)}
           />
           <button type="submit" className="login-button">Kaydet</button>
        </form>
      </div>

      <div className="add-user-section">
        <div className="student-mgmt-table-wrap">
          <table className="student-mgmt-table">
            <thead>
              <tr>
                <th>Öğrenci No</th>
                <th>GitHub Girişi</th>
                <th>Ekleyen</th>
                <th>Tarih</th>
                <th style={{ width: 150 }}>İşlemler</th>
              </tr>
            
              <tr className="filter-row">
                <td>
                  <div className="filter-input-container">
                    <span className="filter-icon">⚙️</span>
                    <input name="studentId" placeholder="Filtrele..." value={filters.studentId} onChange={handleFilterChange} />
                  </div>
                </td>
                <td>
                  <div className="filter-input-container">
                    <span className="filter-icon">⚙️</span>
                    <input name="linked" placeholder="Filtrele..." value={filters.linked} onChange={handleFilterChange} />
                  </div>
                </td>
                <td>
                  <div className="filter-input-container">
                    <span className="filter-icon">⚙️</span>
                    <input name="addedBy" placeholder="Filtrele..." value={filters.addedBy} onChange={handleFilterChange} />
                  </div>
                </td>
                <td>
                  <div className="filter-input-container">
                    <span className="filter-icon">⚙️</span>
                    <input name="addedDate" placeholder="Tarih Seçin..." value={filters.addedDate} onChange={handleFilterChange} />
                  </div>
                </td>
                <td></td>
              </tr>
            </thead>
            <tbody>
              {filteredList.map((row) => (
                <tr key={row.id}>
                  <td>{row.studentId}</td>
                  <td>
                    <span className={`student-mgmt-badge ${row.linked ? 'student-mgmt-badge--linked' : 'student-mgmt-badge--pending'}`}>
                      {row.linked ? 'Yapıldı' : 'Yapılmadı'}
                    </span>
                  </td>
                  <td>{row.addedBy || '—'}</td>
                  <td>{formatDate(row.addedDate)}</td>
                  <td>
                    <button 
                      className="student-mgmt-btn-delete" 
                      disabled={row.linked || deletingId === row.id} 
                      onClick={() => onDelete(row)}
                    >
                      {deletingId === row.id ? '…' : 'Sil'}
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

export default StudentManagement;