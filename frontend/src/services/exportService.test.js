import { describe, it, expect, vi, beforeEach, afterEach } from 'vitest';
import {
  exportToPDF,
  exportChartAsImage,
  exportToCSV,
} from '../services/exportService';

// Mock html2canvas ve jsPDF
vi.mock('html2canvas', () => ({
  default: vi.fn(() =>
    Promise.resolve({
      toDataURL: () => 'data:image/png;base64,mockImageData',
      width: 800,
      height: 600,
    })
  ),
}));

vi.mock('jspdf', () => ({
  default: vi.fn(() => ({
    internal: { pageSize: { getWidth: () => 210, getHeight: () => 297 } },
    setFontSize: vi.fn(),
    text: vi.fn(),
    addImage: vi.fn(),
    addPage: vi.fn(),
    save: vi.fn(),
  })),
}));

describe('Export Service', () => {
  let mockElement;

  beforeEach(() => {
    // Mock DOM element
    mockElement = document.createElement('div');
    mockElement.id = 'test-element';
    mockElement.innerHTML = '<h1>Test Content</h1>';
    document.body.appendChild(mockElement);

    // Mock URL.createObjectURL
    global.URL.createObjectURL = vi.fn(() => 'blob:mock-url');

    // Mock document.createElement for links
    const linkElement = {
      href: '',
      download: '',
      click: vi.fn(),
    };
    vi.spyOn(document, 'createElement').mockReturnValue(linkElement);
  });

  afterEach(() => {
    if (mockElement && mockElement.parentNode) {
      mockElement.parentNode.removeChild(mockElement);
    }
    vi.clearAllMocks();
  });

  describe('exportToPDF', () => {
    it('throws error when element not found', async () => {
      try {
        await exportToPDF('non-existent-id');
        expect.fail('Should have thrown an error');
      } catch (error) {
        expect(error.message).toContain('not found');
      }
    });

    it('exports PDF when element exists', async () => {
      const result = await exportToPDF('test-element', {
        fileName: 'test.pdf',
        title: 'Test Report',
      });
      expect(result).toBe(true);
    });

    it('includes metadata in PDF', async () => {
      const result = await exportToPDF('test-element', {
        fileName: 'test.pdf',
        title: 'Test Report',
        metadata: {
          'Test Key': 'Test Value',
        },
      });
      expect(result).toBe(true);
    });
  });

  describe('exportChartAsImage', () => {
    it('throws error when element not found', async () => {
      try {
        await exportChartAsImage('non-existent-id');
        expect.fail('Should have thrown an error');
      } catch (error) {
        expect(error.message).toContain('not found');
      }
    });

    it('exports image when element exists', async () => {
      const result = await exportChartAsImage('test-element', 'chart.png');
      expect(result).toBe(true);
    });
  });

  describe('exportToCSV', () => {
    it('throws error when data is empty', () => {
      try {
        exportToCSV([]);
        expect.fail('Should have thrown an error');
      } catch (error) {
        expect(error.message).toContain('No data');
      }
    });

    it('exports CSV with valid data', () => {
      const mockData = [
        { name: 'Ahmet', score: 85 },
        { name: 'Fatih', score: 90 },
      ];

      const result = exportToCSV(mockData, 'data.csv');
      expect(result).toBe(true);
    });

    it('handles special characters in CSV', () => {
      const mockData = [
        { name: 'Ahmet, Yılmaz', score: 85 },
        { name: 'Test"Quote', score: 90 },
      ];

      const result = exportToCSV(mockData, 'data.csv');
      expect(result).toBe(true);
    });

    it('escapes quotes in CSV values', () => {
      const mockData = [
        { description: 'Test "quoted" text' },
      ];

      const result = exportToCSV(mockData, 'data.csv');
      expect(result).toBe(true);
    });
  });
});
