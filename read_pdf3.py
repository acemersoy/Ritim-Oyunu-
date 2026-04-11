from pdfminer.high_level import extract_text
import sys

def read_pdf(file_path):
    try:
        text = extract_text(file_path)
        with open("pdf_output_utf8.txt", "w", encoding="utf-8") as f:
            f.write(text)
    except Exception as e:
        print(f"Error extracting PDF: {e}")

if __name__ == "__main__":
    read_pdf(r"c:\Users\aceme\OneDrive\Masaüstü\Proje\KlikleApp (4).pdf")
