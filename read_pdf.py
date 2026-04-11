import sys
import PyPDF2

def read_pdf(file_path):
    try:
        reader = PyPDF2.PdfReader(file_path)
        text = ""
        for i, page in enumerate(reader.pages):
            text += f"--- Page {i+1} ---\n"
            text += page.extract_text() + "\n"
        print(text)
    except Exception as e:
        print(f"Error reading PDF: {e}")

if __name__ == "__main__":
    read_pdf(r"C:\Users\aceme\OneDrive\Masaüstü\KlikleApp (4).pdf")
