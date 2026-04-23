import jsPDF from 'jspdf';
import html2canvas from 'html2canvas';
import { format } from 'date-fns';
import { tr } from 'date-fns/locale';

/**
 * PDF olarak raporu dışa aktarır
 * @param {string} elementId - HTML element ID
 * @param {Object} options - Dışa aktarma seçenekleri
 * @param {string} [options.fileName] - PDF dosya adı
 * @param {string} [options.title] - Rapor başlığı
 * @param {Object} [options.metadata] - Rapor metadatası
 */
export async function exportToPDF(elementId, options = {}) {
  try {
    const element = document.getElementById(elementId);
    if (!element) {
      throw new Error(`Element with ID "${elementId}" not found`);
    }

    // Görseli yakalama
    const canvas = await html2canvas(element, {
      scale: 2,
      logging: false,
      useCORS: true,
    });

    const imgData = canvas.toDataURL('image/png');
    const pdf = new jsPDF({
      orientation: 'portrait',
      unit: 'mm',
      format: 'a4',
    });

    const pdfWidth = pdf.internal.pageSize.getWidth();
    const pdfHeight = pdf.internal.pageSize.getHeight();

    // Başlık ve metadatası ekle
    if (options.title) {
      pdf.setFontSize(16);
      pdf.text(options.title, 10, 10);
      pdf.setFontSize(10);
      pdf.text(
        `Oluşturulma Tarihi: ${format(new Date(), 'dd MMMM yyyy HH:mm', { locale: tr })}`,
        10,
        18
      );

      if (options.metadata) {
        let yPosition = 26;
        Object.entries(options.metadata).forEach(([key, value]) => {
          pdf.text(`${key}: ${value}`, 10, yPosition);
          yPosition += 6;
        });
      }

      pdf.addPage();
    }

    // Görüntüyü PDF'e ekle
    const imgHeight = (canvas.height * pdfWidth) / canvas.width;
    let heightLeft = imgHeight;
    let position = 0;

    pdf.addImage(imgData, 'PNG', 0, position, pdfWidth, imgHeight);
    heightLeft -= pdfHeight;

    while (heightLeft >= 0) {
      position = heightLeft - imgHeight;
      pdf.addPage();
      pdf.addImage(imgData, 'PNG', 0, position, pdfWidth, imgHeight);
      heightLeft -= pdfHeight;
    }

    // PDF'i indir
    const fileName =
      options.fileName ||
      `Performans-Raporu-${format(new Date(), 'yyyy-MM-dd-HHmmss')}.pdf`;
    pdf.save(fileName);

    return true;
  } catch (error) {
    console.error('PDF export error:', error);
    throw error;
  }
}

/**
 * Grafikleri PNG görüntü olarak dışa aktarır
 * @param {string} elementId - HTML element ID
 * @param {string} [fileName] - Dosya adı
 */
export async function exportChartAsImage(elementId, fileName) {
  try {
    const element = document.getElementById(elementId);
    if (!element) {
      throw new Error(`Element with ID "${elementId}" not found`);
    }

    const canvas = await html2canvas(element, {
      scale: 2,
      logging: false,
      useCORS: true,
    });

    const link = document.createElement('a');
    link.href = canvas.toDataURL('image/png');
    link.download =
      fileName || `Grafik-${format(new Date(), 'yyyy-MM-dd-HHmmss')}.png`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    return true;
  } catch (error) {
    console.error('Image export error:', error);
    throw error;
  }
}

/**
 * CSV formatında veriyi dışa aktarır
 * @param {Array} data - Veri dizisi
 * @param {string} [fileName] - Dosya adı
 */
export function exportToCSV(data, fileName) {
  try {
    if (!data || data.length === 0) {
      throw new Error('No data to export');
    }

    const headers = Object.keys(data[0]);
    const csvContent = [
      headers.join(','),
      ...data.map((row) =>
        headers
          .map((header) => {
            const value = row[header];
            // CSV'de virgül ve tırnak içeren değerleri escape et
            if (value === null || value === undefined) {
              return '';
            }
            const stringValue = String(value);
            if (stringValue.includes(',') || stringValue.includes('"')) {
              return `"${stringValue.replace(/"/g, '""')}"`;
            }
            return stringValue;
          })
          .join(',')
      ),
    ].join('\n');

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    const url = URL.createObjectURL(blob);

    link.href = url;
    link.download =
      fileName || `Veri-${format(new Date(), 'yyyy-MM-dd-HHmmss')}.csv`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);

    return true;
  } catch (error) {
    console.error('CSV export error:', error);
    throw error;
  }
}
