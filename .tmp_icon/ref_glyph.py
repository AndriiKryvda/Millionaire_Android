from PIL import Image, ImageDraw, ImageFont

OUT = r"c:\Projects\Vibecoding_Personal\Millionaire\.tmp_icon"
fonts = [
    r"C:\Windows\Fonts\arial.ttf",
    r"C:\Windows\Fonts\segoeui.ttf",
    r"C:\Windows\Fonts\georgia.ttf",
    r"C:\Windows\Fonts\calibri.ttf",
    r"C:\Windows\Fonts\comic.ttf",
]
tiles = []
for f in fonts:
    try:
        img = Image.new("RGB", (300, 300), (8, 8, 46))
        d = ImageDraw.Draw(img)
        try:
            font = ImageFont.truetype(f, 220)
        except Exception:
            continue
        d.text((150, 150), "\u0b52"[:0] + "\u20b4", font=font, fill=(245, 197, 66), anchor="mm")
        tiles.append((f.split("\\")[-1], img))
    except Exception as e:
        print("fail", f, e)

if tiles:
    sheet = Image.new("RGB", (300 * len(tiles), 340), (20, 20, 20))
    for i, (name, im) in enumerate(tiles):
        sheet.paste(im, (300 * i, 0))
        ImageDraw.Draw(sheet).text((300 * i + 10, 305), name, fill=(255, 255, 255))
    sheet.save(OUT + r"\hryvnia_ref.png")
    print("saved", [t[0] for t in tiles])
else:
    print("no fonts rendered")
