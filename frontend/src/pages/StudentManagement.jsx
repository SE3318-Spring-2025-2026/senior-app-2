import { useCallback, useEffect, useRef, useState } from 'react';
import {
  uploadStudentWhitelist,
  getStudentWhitelist,
  deleteStudentWhitelistEntry,
} from '../services/api';
import './Users.css';
import './StudentManagement.css';

/**
 * CSV / metin: her satırdan yalnızca öğrenci numarası (ilk sütun).
 * Yeni yüklemede GitHub durumu CSV’de yok; ek sütunlar varsa yok sayılır.
 */
function parseStudentIdsFromFileText(text) {
  const lines = text.split(/\r?\n/).map((l) => l.trim()).filter((l) => l !== '');
  const ids = [];
  let isFirstData = true;
  const headerCells = new Set([
    'studentid', 'student_id', 'id', 'öğrenci no', 'ogrenci_no', 'ogrenci no',
    'student no', 'student_no', 'numara', 'ogrencino',
  ]);
  for (const line of lines) {
    const rawCell = line.split(/[,;]/)[0];
    const cell = rawCell.replace(/^\uFEFF/, '').replace(/^"|"$/g, '').trim();
    if (!cell) continue;
    if (isFirstData) {
      const lower = cell.toLowerCase();
      if (headerCells.has(lower)) {
        isFirstData = false;
        continue;
      }
    }
    isFirstData = false;
    ids.push(cell);
  }
  return ids;
}

function formatDate(iso) {
  if (!iso) return '—';
  try {
    const s = typeof iso === 'string' ? iso : String(iso);
    const d = new Date(s.includes('T') ? s : `${s}T00:00:00`);
    if (Number.isNaN(d.getTime())) return s;
    return d.toLocaleString();
  } catch {
    return String(iso);
  }
}

function isRowLinked(row) {
  if (!row || typeof row !== 'object') return false;
  return row.linked === true || row.linked === 'true' || row.accountLinked === true;
}

function StudentManagement() {
  const [ids, setIds] = useState('');
  const [status, setStatus] = useState({ type: '', msg: '' });
  const [list, setList] = useState([]);
  const [listLoading, setListLoading] = useState(true);
  const [listError, setListError] = useState('');
  const [deletingId, setDeletingId] = useState(null);
  const [dropActive, setDropActive] = useState(false);
  const fileInputRef = useRef(null);

  const loadList = useCallback(async () => {
    setListLoading(true);
    setListError('');
    try {
      const data = await getStudentWhitelist();
      setList(Array.isArray(data) ? data : []);
    } catch (err) {
      setListError(err.message || 'Liste yüklenemedi.');
      setList([]);
    } finally {
      setListLoading(false);
    }
  }, []);

  useEffect(() => {
    loadList();
  }, [loadList]);

  const runUpload = async (idArray) => {
    const unique = [...new Set(idArray.map((s) => s.trim()).filter((s) => s !== ''))];
    if (unique.length === 0) {
      setStatus({ type: 'error', msg: 'Eklenecek geçerli öğrenci numarası yok.' });
      return;
    }
    try {
      const res = await uploadStudentWhitelist(unique);
      const added = res.added ?? 0;
      setStatus({
        type: 'success',
        msg: `${added} yeni kayıt eklendi (${unique.length} satır gönderildi).`,
      });
      setIds('');
      await loadList();
    } catch (err) {
      setStatus({ type: 'error', msg: err.message || 'Yükleme başarısız.' });
    }
  };

  const handleUpload = async (e) => {
    e.preventDefault();
    const idArray = ids.split(/[\n,]+/).map((s) => s.trim()).filter((s) => s !== '');
    await runUpload(idArray);
  };

  const handleFile = async (file) => {
    if (!file || !file.name.toLowerCase().endsWith('.csv')) {
      setStatus({ type: 'error', msg: 'Lütfen .csv dosyası seçin veya bırakın.' });
      return;
    }
    const text = await file.text();
    const parsed = parseStudentIdsFromFileText(text);
    await runUpload(parsed);
  };

  const onDrop = (e) => {
    e.preventDefault();
    setDropActive(false);
    const f = e.dataTransfer.files?.[0];
    if (f) handleFile(f);
  };

  const onDelete = async (row) => {
    if (isRowLinked(row)) return;
    if (!window.confirm(`Bu öğrenci numarasını listeden kaldırmak istiyor musunuz?\n${row.studentId}`)) {
      return;
    }
    setDeletingId(row.id);
    try {
      await deleteStudentWhitelistEntry(row.id);
      setStatus({ type: 'success', msg: 'Kayıt silindi.' });
      await loadList();
    } catch (err) {
      setStatus({ type: 'error', msg: err.message || 'Silinemedi.' });
    } finally {
      setDeletingId(null);
    }
  };

  return (
    <div className="users-page">
      <h1>Koordinatör: Öğrenci beyaz liste</h1>
      <div className="add-user-section">
        <p className="student-mgmt-muted student-mgmt-intro">
          CSV yalnızca öğrenci numarası içerir (tek sütun veya satır başına bir numara); yeni eklenenlerde GitHub girişi listede otomatik <strong>Yapılmadı</strong> görünür.
          Metin kutusuna da yapıştırabilirsiniz.
        </p>

        <div
          role="button"
          tabIndex={0}
          className={`student-mgmt-dropzone ${dropActive ? 'student-mgmt-dropzone--active' : ''}`}
          onClick={() => fileInputRef.current?.click()}
          onKeyDown={(e) => {
            if (e.key === 'Enter' || e.key === ' ') {
              e.preventDefault();
              fileInputRef.current?.click();
            }
          }}
          onDragEnter={(e) => { e.preventDefault(); e.stopPropagation(); setDropActive(true); }}
          onDragOver={(e) => { e.preventDefault(); e.stopPropagation(); setDropActive(true); }}
          onDragLeave={(e) => {
            if (!e.currentTarget.contains(e.relatedTarget)) setDropActive(false);
          }}
          onDrop={onDrop}
        >
          <input
            ref={fileInputRef}
            type="file"
            className="student-mgmt-dropzone-file"
            accept=".csv,text/csv"
            onChange={(e) => {
              const f = e.target.files?.[0];
              e.target.value = '';
              if (f) handleFile(f);
            }}
          />
          <div className="student-mgmt-dropzone-inner">
            <strong>CSV sürükleyip bırakın</strong>
            <span className="student-mgmt-muted">veya tıklayıp .csv seçin — sadece öğrenci no (tek sütun)</span>
          </div>
        </div>

        <form onSubmit={handleUpload}>
          <textarea
            style={{
              width: '100%',
              minHeight: '150px',
              padding: '12px',
              borderRadius: '8px',
              border: '1px solid #ddd',
              marginBottom: '15px',
              display: 'block',
              fontFamily: 'inherit',
            }}
            placeholder="Öğrenci numaraları: virgül, boşluk veya satır satır (CSV ile aynı mantık)..."
            value={ids}
            onChange={(e) => setIds(e.target.value)}
          />
          <button type="submit" className="login-button" style={{ width: 'auto' }}>
            Geçerli numaraları kaydet
          </button>
        </form>
        {status.msg && (
          <div
            className={status.type === 'success' ? 'role-badge' : 'users-error'}
            style={{ marginTop: '15px' }}
          >
            {status.msg}
          </div>
        )}
      </div>

      <div className="add-user-section">
        <h2 style={{ marginTop: 0 }}>Kayıtlı öğrenci numaraları</h2>
        <p className="student-mgmt-muted">GitHub ile giriş yapılmış kayıtlar silinemez.</p>
        {listLoading && <div className="users-loading" style={{ padding: '24px' }}>Yükleniyor…</div>}
        {listError && <div className="users-error">{listError}</div>}
        {!listLoading && !listError && list.length === 0 && (
          <p className="student-mgmt-muted">Henüz kayıt yok.</p>
        )}
        {!listLoading && list.length > 0 && (
          <div className="student-mgmt-table-wrap">
            <table className="student-mgmt-table">
              <thead>
                <tr>
                  <th>Öğrenci no</th>
                  <th>GitHub girişi</th>
                  <th>Ekleyen</th>
                  <th>Tarih</th>
                  <th style={{ width: 100 }} />
                </tr>
              </thead>
              <tbody>
                {list.map((row) => (
                  <tr key={row.id}>
                    <td>{row.studentId}</td>
                    <td>
                      {isRowLinked(row) ? (
                        <span className="student-mgmt-badge student-mgmt-badge--linked">Yapıldı</span>
                      ) : (
                        <span className="student-mgmt-badge student-mgmt-badge--pending">Yapılmadı</span>
                      )}
                    </td>
                    <td>{row.addedBy || '—'}</td>
                    <td>{formatDate(row.addedDate)}</td>
                    <td>
                      <button
                        type="button"
                        className="student-mgmt-btn-delete"
                        disabled={isRowLinked(row) || deletingId === row.id}
                        title={isRowLinked(row) ? 'GitHub ile giriş yapılmış kayıt silinemez' : 'Listeden kaldır'}
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
        )}
      </div>
    </div>
  );
}

export default StudentManagement;
