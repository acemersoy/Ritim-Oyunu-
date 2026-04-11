import fitz
import sys

def read_pdf(file_path):
    try:
        doc = fitz.open(file_path)
        text = ""
        for page in doc:
            text += f"--- Page {page.number+1} ---\n"
            text += page.get_text() + "\n"
        print(text)
    except Exception as e:
        print(f"Error reading PDF: {e}")

if __name__ == "__main__":
    read_pdf(r"C:\Users\aceme\OneDrive\Masaüstü\KlikleApp (4).pdf")
